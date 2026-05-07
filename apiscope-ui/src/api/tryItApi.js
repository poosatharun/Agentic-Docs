import { apiClient } from './client'

/** HTTP methods that may carry a request body. */
export const BODY_METHODS = new Set(['POST', 'PUT', 'PATCH'])

/**
 * Executes a live HTTP request against the target API endpoint.
 *
 * @param {{
 *   path:       string,
 *   httpMethod: string,
 *   pathParams: Record<string, string>,
 *   body:       string,
 * }} params
 * @returns {Promise<{ status: number, ok: boolean, body: string }>}
 */
export async function executeTryIt({ path, httpMethod, pathParams, body }) {
  // Substitute path parameters into the URL template
  const url = Object.entries(pathParams).reduce(
    (acc, [key, value]) => acc.replace(`{${key}}`, value || key),
    path,
  )

  const options = { method: httpMethod }

  if (BODY_METHODS.has(httpMethod) && body.trim()) {
    options.body = body
  }

  try {
    const response = await fetch(url, {
      ...options,
      headers: { 'Content-Type': 'application/json' },
    })
    const text = await response.text()
    let pretty
    try { pretty = JSON.stringify(JSON.parse(text), null, 2) } catch { pretty = text }
    return { status: response.status, ok: response.ok, body: pretty }
  } catch (err) {
    return { status: 0, ok: false, body: err.message }
  }
}
