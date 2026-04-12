"use client";

import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  type CarouselApi,
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import { cn } from "@/lib/utils";
import { ChevronLeft, ChevronRight } from "lucide-react";
import Image from "next/image";
import { useEffect, useState } from "react";

interface BookImageCarouselProps {
  title: string;
  mediaUrls: string[];
}

export function BookImageCarousel({ title, mediaUrls }: BookImageCarouselProps) {
  const [api, setApi] = useState<CarouselApi>();
  const [currentSlide, setCurrentSlide] = useState(0);
  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerIndex, setViewerIndex] = useState(0);

  useEffect(() => {
    if (!api) return;

    const sync = () => {
      setCurrentSlide(api.selectedScrollSnap());
    };

    sync();
    api.on("select", sync);
    api.on("reInit", sync);

    return () => {
      api.off("select", sync);
      api.off("reInit", sync);
    };
  }, [api]);

  function openViewer(index: number) {
    setViewerIndex(index);
    setViewerOpen(true);
  }

  function goPrevInViewer() {
    setViewerIndex((prev) => (prev === 0 ? mediaUrls.length - 1 : prev - 1));
  }

  function goNextInViewer() {
    setViewerIndex((prev) => (prev === mediaUrls.length - 1 ? 0 : prev + 1));
  }

  if (mediaUrls.length === 0) {
    return (
      <div className="flex aspect-[4/3] items-center justify-center border border-border bg-muted text-sm text-muted-foreground md:aspect-square">
        No images available
      </div>
    );
  }

  return (
    <>
    <Carousel
      className="w-full"
      opts={{ loop: mediaUrls.length > 1 }}
      setApi={setApi}
    >
      <CarouselContent>
        {mediaUrls.map((url, index) => (
          <CarouselItem key={`${url}-${index}`}>
            <button
              type="button"
              className="relative block aspect-[4/3] w-full overflow-hidden border border-border bg-muted md:aspect-square"
              onClick={() => openViewer(index)}
            >
              <Image
                src={url}
                alt={`${title} image ${index + 1}`}
                fill
                unoptimized
                className="object-cover transition-transform duration-200 hover:scale-[1.02]"
                sizes="(max-width: 768px) 100vw, 40vw"
              />
            </button>
          </CarouselItem>
        ))}
      </CarouselContent>

      {mediaUrls.length > 1 ? (
        <>
          <CarouselPrevious className="left-2" />
          <CarouselNext className="right-2" />
        </>
      ) : null}

      {mediaUrls.length > 1 ? (
        <div className="mt-3 flex items-center justify-center gap-2">
          {mediaUrls.map((_, index) => (
            <button
              key={`dot-${index}`}
              type="button"
              aria-label={`Go to slide ${index + 1}`}
              onClick={() => api?.scrollTo(index)}
              className={cn(
                "h-1.5 w-6 border border-border transition-colors",
                currentSlide === index ? "bg-foreground" : "bg-transparent",
              )}
            />
          ))}
        </div>
      ) : null}
    </Carousel>
    <Dialog open={viewerOpen} onOpenChange={setViewerOpen}>
      <DialogContent className="max-w-5xl p-2 sm:p-3" showCloseButton>
        <DialogTitle className="sr-only">Image viewer</DialogTitle>
        <div className="relative flex items-center justify-center bg-muted">
          <div className="relative max-h-[80vh] w-full">
            <Image
              src={mediaUrls[viewerIndex]}
              alt={`${title} image ${viewerIndex + 1}`}
              width={1800}
              height={1200}
              unoptimized
              className="mx-auto max-h-[80vh] w-auto object-contain"
            />
          </div>

          {mediaUrls.length > 1 ? (
            <>
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={goPrevInViewer}
                className="absolute left-3 top-1/2 -translate-y-1/2"
              >
                <ChevronLeft />
              </Button>
              <Button
                type="button"
                variant="outline"
                size="icon"
                onClick={goNextInViewer}
                className="absolute right-3 top-1/2 -translate-y-1/2"
              >
                <ChevronRight />
              </Button>
            </>
          ) : null}
        </div>
      </DialogContent>
    </Dialog>
    </>
  );
}
