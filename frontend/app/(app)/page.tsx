import { HomeFeed } from "@/features/home/components/HomeFeed";
import { getHomeFeed } from "@/features/home/server/home.api";
import { getNavbarSnapshot } from "@/features/navbar/server/navbar.api";

export default async function HomePage() {
  const [books, snapshot] = await Promise.all([
    getHomeFeed(20),
    getNavbarSnapshot(),
  ]);

  return <HomeFeed books={books} currentUserId={snapshot.userId} />;
}
