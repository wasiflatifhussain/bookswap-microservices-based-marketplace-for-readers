"use client";

import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import {
  UploadCompleteResponse,
  UploadInitResponse,
} from "@/features/library/types";
import { useRouter } from "next/navigation";
import { ChangeEvent, FormEvent, useEffect, useMemo, useState } from "react";

type FormState = {
  title: string;
  description: string;
  genre: string;
  author: string;
  bookCondition: string;
};

const initialForm: FormState = {
  title: "",
  description: "",
  genre: "",
  author: "",
  bookCondition: "",
};

const acceptedMimeTypes = new Set(["image/png", "image/jpeg"]);
const GENRE_OPTIONS = [
  "FANTASY",
  "SCIENCE_FICTION",
  "MYSTERY",
  "THRILLER",
  "ROMANCE",
  "HORROR",
  "HISTORICAL_FICTION",
  "LITERARY_FICTION",
  "YOUNG_ADULT",
  "BIOGRAPHY_MEMOIR",
  "HISTORY",
  "SELF_HELP",
  "SCIENCE_NATURE",
  "BUSINESS_FINANCE",
  "COOKING",
  "GRAPHIC_NOVEL",
  "CHILDRENS",
  "COMICS",
] as const;
const CONDITION_OPTIONS = ["NEW", "LIKE_NEW", "GOOD", "FAIR", "POOR"] as const;
const AI_PENDING_VALUATION = 0.01;

function formatEnumLabel(value: string): string {
  return value
    .split("_")
    .map((part) => part.charAt(0) + part.slice(1).toLowerCase())
    .join(" ");
}

function isImageTypeValid(file: File): boolean {
  return acceptedMimeTypes.has(file.type);
}

export function CreateBookForm() {
  const router = useRouter();
  const [form, setForm] = useState<FormState>(initialForm);
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [statusText, setStatusText] = useState<string>("");
  const [error, setError] = useState<string | null>(null);

  const previewUrls = useMemo(
    () => files.map((file) => URL.createObjectURL(file)),
    [files],
  );

  useEffect(() => {
    return () => {
      previewUrls.forEach((url) => URL.revokeObjectURL(url));
    };
  }, [previewUrls]);

  function updateField<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((prev) => ({ ...prev, [key]: value }));
  }

  function handleFiles(event: ChangeEvent<HTMLInputElement>) {
    const list = event.target.files;
    if (!list) return;

    const selected = Array.from(list);
    const invalid = selected.find((file) => !isImageTypeValid(file));
    if (invalid) {
      setError("Only PNG and JPEG files are supported.");
      return;
    }

    setError(null);
    setFiles((prev) => {
      const combined = [...prev, ...selected];
      const deduped: File[] = [];
      const seen = new Set<string>();

      for (const file of combined) {
        const key = `${file.name}-${file.size}-${file.lastModified}`;
        if (seen.has(key)) continue;
        seen.add(key);
        deduped.push(file);
        if (deduped.length >= 8) break;
      }

      return deduped;
    });

    // Allow selecting the same file again in a later pick event.
    event.target.value = "";
  }

  function removeFile(indexToRemove: number) {
    setFiles((prev) => prev.filter((_, index) => index !== indexToRemove));
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setStatusText("");

    if (files.length === 0) {
      setError("Please upload at least one image.");
      return;
    }

    const fileItems = files.map((file, index) => ({
      clientRef: `file-${index}`,
      name: file.name,
      sizeBytes: file.size,
      mimeType: file.type,
    }));

    setSubmitting(true);

    try {
      setStatusText("Creating listing and generating upload URLs...");
      const initRes = await fetch("/api/bff/books/create/init", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          title: form.title.trim(),
          description: form.description.trim(),
          genre: form.genre.trim(),
          author: form.author.trim(),
          bookCondition: form.bookCondition.trim(),
          valuation: AI_PENDING_VALUATION,
          mediaIds: [],
          files: fileItems,
        }),
      });

      if (!initRes.ok) {
        throw new Error(`Failed to initialize upload (${initRes.status})`);
      }

      const initPayload = (await initRes.json()) as UploadInitResponse;
      if (!initPayload.bookId || !Array.isArray(initPayload.results)) {
        throw new Error("Invalid init response from server");
      }

      const readyItems = initPayload.results.filter((item) => item.status === "READY");
      if (readyItems.length === 0) {
        const firstFailure = initPayload.results.find((item) => item.status !== "READY");
        throw new Error(firstFailure?.errorMessage || "No files were ready for upload");
      }

      setStatusText("Uploading images to storage...");
      const successfulMediaIds: string[] = [];

      await Promise.all(
        readyItems.map(async (item) => {
          const index = Number(item.clientRef?.split("-")[1]);
          const file = files[index];
          if (!file || !item.presignedPutUrl || !item.mediaId) {
            return;
          }

          const uploadRes = await fetch(item.presignedPutUrl, {
            method: "PUT",
            headers: {
              "Content-Type":
                item.requiredHeaders?.contentType || file.type || "application/octet-stream",
            },
            body: file,
          });

          if (!uploadRes.ok) {
            throw new Error(`Upload failed for ${file.name}`);
          }

          successfulMediaIds.push(item.mediaId);
        }),
      );

      if (successfulMediaIds.length === 0) {
        throw new Error("Image upload failed for all files");
      }

      if (successfulMediaIds.length !== readyItems.length) {
        throw new Error("Some image uploads failed. Please try again.");
      }

      setStatusText("Finalizing listing...");
      const completeRes = await fetch("/api/bff/books/create/complete", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          bookId: initPayload.bookId,
          mediaIds: successfulMediaIds,
        }),
      });

      if (!completeRes.ok) {
        throw new Error(`Failed to complete listing (${completeRes.status})`);
      }

      const completePayload = (await completeRes.json()) as UploadCompleteResponse;
      if ((completePayload.successCount ?? 0) <= 0) {
        throw new Error("Listing was created but media confirmation failed");
      }

      setStatusText("Listing created successfully. Redirecting...");
      router.replace("/library");
      return;
    } catch (e: unknown) {
      setStatusText("");
      if (e instanceof Error) setError(e.message);
      else setError("Failed to create listing.");
    } finally {
      setStatusText("");
      setSubmitting(false);
    }
  }

  return (
    <>
      {statusText ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/55 px-4 backdrop-blur-[1.5px]">
          <div className="w-full max-w-lg border border-border bg-card px-7 py-6">
            <p className="text-xs font-medium uppercase tracking-[0.12em] text-muted-foreground">
              BookSwap
            </p>
            <p className="mt-1 text-lg font-semibold tracking-tight text-foreground">
              Publishing your listing
            </p>

            <div className="mt-4 flex items-center gap-3 border border-border bg-muted/35 px-4 py-3">
              <div className="h-5 w-5 animate-spin rounded-full border-2 border-muted-foreground/30 border-t-foreground" />
              <p className="text-sm text-foreground" aria-live="polite">
                {statusText}
              </p>
            </div>
          </div>
        </div>
      ) : null}

      <Card className="surface-card rounded-md p-5 md:p-6">
        <form onSubmit={handleSubmit} className="space-y-5">
        {error ? (
          <p className="rounded-sm border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive">
            {error}
          </p>
        ) : null}

        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <div className="space-y-1">
            <label className="text-sm font-medium">Title</label>
            <input
              required
              className="input-field"
              value={form.title}
              onChange={(e) => updateField("title", e.target.value)}
              placeholder="Book title"
              disabled={submitting}
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Author</label>
            <input
              required
              className="input-field"
              value={form.author}
              onChange={(e) => updateField("author", e.target.value)}
              placeholder="Author name"
              disabled={submitting}
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Genre</label>
            <select
              required
              className="input-field"
              value={form.genre}
              onChange={(e) => updateField("genre", e.target.value)}
              disabled={submitting}
            >
              <option value="" disabled>
                Select a genre
              </option>
              {GENRE_OPTIONS.map((genre) => (
                <option key={genre} value={genre}>
                  {formatEnumLabel(genre)}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Condition</label>
            <select
              required
              className="input-field"
              value={form.bookCondition}
              onChange={(e) => updateField("bookCondition", e.target.value)}
              disabled={submitting}
            >
              <option value="" disabled>
                Select condition
              </option>
              {CONDITION_OPTIONS.map((condition) => (
                <option key={condition} value={condition}>
                  {formatEnumLabel(condition)}
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-1 md:col-span-2">
            <label className="text-sm font-medium">Description</label>
            <textarea
              required
              className="input-field min-h-28 resize-y py-2"
              value={form.description}
              onChange={(e) => updateField("description", e.target.value)}
              placeholder="Describe your book"
              disabled={submitting}
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Valuation (BookCoins)</label>
            <input
              className="input-field bg-muted/40 text-muted-foreground"
              value="Valuation will be available by AI"
              disabled
              readOnly
            />
            <p className="text-xs text-muted-foreground">
              The final BookCoin value is computed after upload processing.
            </p>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Images (PNG/JPEG)</label>
            <input
              type="file"
              multiple
              accept="image/png,image/jpeg"
              onChange={handleFiles}
              disabled={submitting}
              className="block h-11 w-full border border-border bg-card px-3 py-2 text-sm text-foreground file:mr-3 file:border-0 file:bg-transparent file:text-sm file:font-medium"
            />
          </div>
        </div>

        {previewUrls.length > 0 ? (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            {previewUrls.map((url, index) => (
              <div
                key={`preview-${index}`}
                className="relative aspect-square overflow-hidden border border-border bg-muted"
              >
                <button
                  type="button"
                  onClick={() => removeFile(index)}
                  disabled={submitting}
                  className="absolute right-1 top-1 z-10 h-7 w-7 border border-border bg-card/95 text-sm font-semibold text-foreground transition hover:bg-card disabled:cursor-not-allowed disabled:opacity-60"
                  aria-label={`Remove image ${index + 1}`}
                  title="Remove image"
                >
                  ×
                </button>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src={url}
                  alt={`Preview ${index + 1}`}
                  className="h-full w-full object-cover"
                />
              </div>
            ))}
          </div>
        ) : null}

        <div className="flex justify-end gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => router.push("/library")}
            disabled={submitting}
          >
            Cancel
          </Button>
          <Button type="submit" disabled={submitting}>
            {submitting ? "Creating..." : "Create Listing"}
          </Button>
        </div>
        </form>
      </Card>
    </>
  );
}
