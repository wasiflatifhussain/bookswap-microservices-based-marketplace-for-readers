# Valuation Service

## Main Tasks

1. **Valuation on Book Media Finalization**
    - Listens to `catalog.events` Kafka topic for `BOOK_MEDIA_FINALIZED` events.
    - Retrieves the book object from the event payload.
    - Contacts the Media Service to fetch associated media for the book.
    - Passes the book and media data to an AI model to compute the book's valuation in book coins.

2. **Unlisting on Book Unlisted Event**
    - Listens to `catalog.events` Kafka topic for `BOOK_UNLISTED` events.
    - Unlists the corresponding book in the valuation system.

---

## Description

The Valuation Service is responsible for computing and updating the valuation of books in the system. It listens to
specific events in the Kafka `catalog.events` topic and performs actions such as valuing a book when its media is
finalized and unlisting a book when it is no longer available.

---

## How It Works

- The service subscribes to the `catalog.events` Kafka topic.
- On receiving a `BOOK_MEDIA_FINALIZED` event, it fetches the necessary data and computes the book's valuation.
- If a `BOOK_UNLISTED` event is received, the service updates the book's status in the valuation system to unlisted.

---

## Technical Notes & Implementation Details

### Blocking vs Non-Blocking

- The external caller client code may be reactive/non-blocking, but since we use JPARepository in the Service layer, the
  service layer is blocking as JPARepo is blocking.
- To make the whole service non-blocking, use R2dbcRepo or ReactiveMongoRepo, etc.
- Non-blocking/reactive/async means the method will free up threads while waiting for responses.

### Service-to-Service Authentication

- Since we are calling service to service with no API calls, we don't have auth tokens, so media service will return
    403.
- To avoid that, we made a new Keycloak client called `service-service-comms` and use that to obtain a service token and
  pass that during web-config setup, which allows verification and permissions.

---

## End-to-End Valuation Workflow

1. **Event Reception**
    - After valuation service receives a `BOOK_MEDIA_FINALIZED` event, it calls media service to get media details.
2. **Media Retrieval**
    - It gets presigned URLs of the media files from the media service.
    - Based on these files, it calls GeminiCaller, which does the majority of Gemini API calling tasks.
3. **Valuation Prompt Preparation**
    - Gemini caller service calls the valuation prompt provider, which makes a prompt with placeholders and fills them
      with book details.
    - Based on the media files, it calls each presigned URL with GET to download the bytes of the files.
4. **Gemini API Call**
    - Gemini caller service then passes the prompt and the image byte data together to Gemini client
      `getAnswerWithImagesAndSearch`, which makes a Gemini request object with these details, fills placeholders for
      image bytes, and calls Gemini API.
    - The Gemini API returns a response, which is filtered and cleaned, and finally returns a Gemini response.
5. **Persistence and Event Publishing**
    - The response comes back to valuation service, which then saves it to DB and publishes to Kafka.
    - Catalog service listens to that event and updates the book object with the valuation details.

---

## How it all works and all features

After valuation service receives `BOOK_MEDIA_FINALIZED` event, it calls media service to get media details. From that it
gets presigned url of the media files. Based on these files, it calls GeminiCaller and that one by one with GET to
download the bytes of the files. Then it calls the Gemini API with book objects, as well as the media file bytes. It
makes a detailed prompt with all these details and asks Gemini to give a valuation in book coins in a specific format.

For details:

- Valuation service calls media service from client folder and calls `getMediaByBookId` method.
- Then it gets the presigned URLs from the media objects.
- Then it calls Gemini caller service which does the heavy API call related tasks.
- It calls `downloadFile` method in the media download client to download the files one by one.
- It calls valuation prompt provider who makes a prompt with placeholders and fills the placeholders with book details.
- Then it passes the prompt and the image byte data together to Gemini client `getAnswerWithImagesAndSearch` who first
  makes a Gemini request object with these details. There it makes placeholders for image bytes and fills them with the
  image byte data.
- Then it calls Gemini API and gets the response.
- Then it does a lot of filtering and cleaning of the data and finally returns Gemini response.
- The response comes back to valuation service who then saves to DB and publishes to Kafka.
- Then catalog service listens to that event and updates the book object with the valuation details.

---

## No Controller-based Service

This service does not have any controllers as it is not called by any external caller. It only listens to Kafka events
from catalog service, calls media service and Gemini API, and publishes events to Kafka and its own DB. The catalog
service will receive valuation details from the event and update the book object. Hence, that eliminates the need for
any controller in this service.

## Running Locally

Update your IDE's run configuration to include the following environment variables:

| Variable         | Description                                          |
|:-----------------|:-----------------------------------------------------|
| `GEMINI_API_KEY` | Your personal API key for the Google Gemini API.     |
| `GEMINI_API_URL` | The endpoint URL for the Gemini model you are using. |

## VPN Requirement

**VVVIP Note:**

- As we are calling Gemini from Hong Kong, we need to use a VPN with Singapore or another region to get access to the
  API.
- Use a complete computer-wide VPN and not a browser extension.

---