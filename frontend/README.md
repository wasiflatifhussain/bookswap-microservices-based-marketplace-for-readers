```
bookswap-web/
├── app/
│   ├── (public)/
│   │   └── page.tsx                    # Home (feed)
│   │
│   ├── library/
│   │   └── page.tsx                    # My Library
│   │
│   ├── book/
│   │   └── [bookId]/
│   │       └── page.tsx                # Book detail page
│   │
│   ├── swap/
│   │   ├── page.tsx                    # Swap Center (sent / received)
│   │   └── create/
│   │       └── page.tsx                # Create swap flow
│   │
│   ├── layout.tsx                      # App shell (Navbar)
│   └── globals.css
│
├── features/
│   ├── navbar/
│   │   ├── components/
│   │   │   ├── Navbar.tsx
│   │   │   ├── NotificationBell.tsx
│   │   │   └── NotificationList.tsx
│   │   ├── server/
│   │   │   └── navbar.api.ts
│   │   └── types.ts
│   │
│   ├── home/
│   │   ├── components/
│   │   │   └── HomeFeed.tsx
│   │   ├── server/
│   │   │   └── home.api.ts
│   │   └── types.ts
│   │
│   ├── catalog/
│   │   ├── components/
│   │   │   ├── BookCard.tsx
│   │   │   ├── BookList.tsx
│   │   │   ├── BookCarousel.tsx
│   │   │   └── BookActions.tsx
│   │   ├── server/
│   │   │   └── catalog.api.ts
│   │   └── types.ts
│   │
│   ├── library/
│   │   ├── components/
│   │   │   └── MyBooksSection.tsx
│   │   ├── server/
│   │   │   └── library.api.ts
│   │   └── types.ts
│   │
│   ├── swap/
│   │   ├── components/
│   │   │   ├── SwapCard.tsx
│   │   │   ├── SwapList.tsx
│   │   │   ├── SwapTabs.tsx
│   │   │   └── SwapBookSelector.tsx
│   │   ├── server/
│   │   │   └── swap.api.ts
│   │   └── types.ts
│   │
│   ├── wallet/
│   │   ├── components/
│   │   │   └── WalletBalance.tsx
│   │   ├── server/
│   │   │   └── wallet.api.ts
│   │   └── types.ts
│
├── components/
│   ├── ui/                             # shadcn only
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── dialog.tsx
│   │   ├── badge.tsx
│   │   └── carousel.tsx
│   │
│   └── layout/
│       └── PageContainer.tsx
│
├── lib/
│   ├── bff-client.ts
│   ├── fetcher.ts
│   └── auth.ts
│
├── middleware.ts
├── tailwind.config.ts
└── package.json

```

## Getting Started

First, run the development server:

```bash
npm run dev
# or
yarn dev
# or
pnpm dev
# or
bun dev
```

Open [http://localhost:3000](http://localhost:3000) with your browser to see the result.

You can start editing the page by modifying `app/page.tsx`. The page auto-updates as you edit the file.

This project uses [`next/font`](https://nextjs.org/docs/app/building-your-application/optimizing/fonts) to automatically optimize and load [Geist](https://vercel.com/font), a new font family for Vercel.

## Learn More

To learn more about Next.js, take a look at the following resources:

- [Next.js Documentation](https://nextjs.org/docs) - learn about Next.js features and API.
- [Learn Next.js](https://nextjs.org/learn) - an interactive Next.js tutorial.

You can check out [the Next.js GitHub repository](https://github.com/vercel/next.js) - your feedback and contributions are welcome!

## Deploy on Vercel

The easiest way to deploy your Next.js app is to use the [Vercel Platform](https://vercel.com/new?utm_medium=default-template&filter=next.js&utm_source=create-next-app&utm_campaign=create-next-app-readme) from the creators of Next.js.

Check out our [Next.js deployment documentation](https://nextjs.org/docs/app/building-your-application/deploying) for more details.
