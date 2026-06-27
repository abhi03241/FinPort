import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Proxy API + auth + swagger to the Spring Boot backend
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: false,
      },
      "/actuator": {
        target: "http://localhost:8080",
        changeOrigin: false,
      },
      "/authenticateTheUser": {
        target: "http://localhost:8080",
        changeOrigin: false,
      },
      "/logout": {
        target: "http://localhost:8080",
        changeOrigin: false,
      },
    },
  },
  build: {
    outDir: "dist",
    sourcemap: true,
  },
});