package com.bookswap.valuation_service.service.prompt;

import com.bookswap.valuation_service.client.media.dto.MediaResponse;
import com.bookswap.valuation_service.dto.event.BookFinalizedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValuationPromptProvider {

  private final ObjectMapper objectMapper;

  private static final String TEMPLATE =
      """
      You are a valuation engine for a book marketplace. You receive:
      1) A finalized book object (JSON)
      2) A list of media objects with image URLs (we attach image bytes for you to inspect)
      3) You MAY use web search tools to find current brand-new prices. Prefer HK sources, else global.

      Instructions:
      - If image bytes are present, inspect them. If not, say: "images_unavailable_for_inspection; inferred from text".
      - Use web search to find 2–3 current brand-new prices. Convert non-HKD to HKD (state the FX rate you assume).
      - If search results are sparse, use best available single source but explain limitations.
      - Output STRICT JSON only (no extra text).

      ---
      ### INPUTS
      - book_json:
      %s

      - media_json:
      %s

      ---
      ### DEFINITIONS
      - Declared condition: NEW | LIKE_NEW | GOOD | FAIR | POOR.
      - Actual condition: your judgment from images + description.
      - Conversion: BookCoins = fair_value_hkd / 10. (BookCoins may include decimals, rounding is not required.)

      ---
      ### TASKS
      1) Parse book details (title, author, genre, description, condition, edition/signed hints).
      2) Images
         - Verify cover/title/author match when possible.
         - Check edges, spine, pages, annotations, yellowing, dust jacket, damage, stickers.
         - Produce: actual_condition (NEW|LIKE_NEW|GOOD|FAIR|POOR) + condition_confidence (0–1).
         - Add any image-based observations and warnings.
      3) Brand-new price (WITH WEB SEARCH)
         - Find 2–3 sources; record source name, URL, listed_price (amount + currency), and converted_hkd.
         - Compute brand_new_price_hkd as median (or best single).
      4) Adjustments
         - Start from brand_new_price_hkd.
         - Baseline multipliers:
           NEW 0.85–0.95 | LIKE_NEW 0.75–0.85 | GOOD 0.55–0.70 | FAIR 0.35–0.50 | POOR 0.15–0.30.
         - Rarity & attributes:
           • First edition +10%%–300%%
           • Signed +15%%–150%%
           • Special/limited +10%%–100%%
           • Damage −10%%–−60%%
         - Output each adjustment: { reason, percent, note }.
      5) Compute final values
         - fair_value_hkd = brand_new_price × adjustments.
         - bookCoins = fair_value_hkd / 10. (Keep decimals, do not round.)
      6) Comments
         - 4–6 sentences explaining the reasoning and any assumptions.
      7) STRICT JSON ONLY.

      ### OUTPUT FORMAT (STRICT JSON)
      {
        "status": "OK",
        "book_id": "<from book_json.bookId>",
        "detected": {
          "title_match_confidence": 0.0,
          "actual_condition": "NEW | LIKE_NEW | GOOD | FAIR | POOR",
          "condition_confidence": 0.0,
          "image_findings": [],
          "special_attributes": [],
          "warnings": []
        },
        "market_prices": {
          "brand_new_sources": [
            {
              "source": "Retailer name",
              "url": "https://...",
              "listed_price": { "amount": 0.00, "currency": "HKD|USD|..." },
              "converted_hkd": 0.00
            }
          ],
          "brand_new_price_hkd": 0.00
        },
        "adjustments": [],
        "fair_value_hkd": 0.00,
        "bookCoins": 0,
        "comments": "Short paragraph summarizing valuation reasoning."
      }
      """;

  public String buildPrompt(BookFinalizedEvent book, List<MediaResponse> media) {
    try {
      String bookJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(book);
      String mediaJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(media);
      log.info("Successfully serialized prompt inputs for bookId={}", book.getBookId());
      return String.format(TEMPLATE, bookJson, mediaJson);
    } catch (Exception e) {
      log.error(
          "Failed to serialize prompt inputs for bookId={}",
          book != null ? book.getBookId() : "unknown",
          e);
      return String.format(TEMPLATE, "{}", "[]");
    }
  }
}
