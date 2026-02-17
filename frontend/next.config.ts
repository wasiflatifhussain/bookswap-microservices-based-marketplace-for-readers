import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async rewrites() {
    return [
      // BFF API routes
      {
        source: "/api/bff/:path*",
        destination: "http://localhost:8080/api/bff/:path*",
      },
    ];
  },
};

export default nextConfig;
