/**
 * Base URLs for the Agentic Docs backend API.
 *
 * Override the base URL at build time by setting VITE_API_BASE_URL in your
 * environment file (see .env.example). Defaults to '' so that the Vite dev
 * server proxy handles all /agentic-docs/api/* requests transparently.
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export const CHAT_URL      = `${BASE_URL}/agentic-docs/api/chat`
export const ENDPOINTS_URL = `${BASE_URL}/agentic-docs/api/endpoints`
