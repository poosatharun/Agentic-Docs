import { useState, useRef, useEffect, useCallback } from 'react'
import ReactMarkdown from 'react-markdown'
import {
  Send, Bot, User, Zap, Code2, ChevronRight, RotateCcw,
  BookOpen, Play, ChevronDown, ChevronUp, Loader2, Search
} from 'lucide-react'

const CHAT_URL = '/agentic-docs/api/chat'
const ENDPOINTS_URL = '/agentic-docs/api/endpoints'

const SUGGESTIONS = [
  'How do I cancel a subscription with a partial refund?',
  'Show me how to process a payment with the API',
  'What endpoints handle refund calculation?',
  'How do I upgrade a user\'s subscription plan?',
  'Generate Java code to get subscription details',
  'What request body does the terminate endpoint need?',
]

const METHOD_COLORS = {
  GET:    { bg: 'bg-emerald-600', text: 'text-white', border: 'border-emerald-500', light: 'bg-emerald-900/20 border-emerald-700/40' },
  POST:   { bg: 'bg-blue-600',   text: 'text-white', border: 'border-blue-500',   light: 'bg-blue-900/20 border-blue-700/40' },
  PUT:    { bg: 'bg-amber-600',  text: 'text-white', border: 'border-amber-500',  light: 'bg-amber-900/20 border-amber-700/40' },
  PATCH:  { bg: 'bg-orange-600', text: 'text-white', border: 'border-orange-500', light: 'bg-orange-900/20 border-orange-700/40' },
  DELETE: { bg: 'bg-red-600',    text: 'text-white', border: 'border-red-500',    light: 'bg-red-900/20 border-red-700/40' },
}
const methodColor = (m) => METHOD_COLORS[m?.toUpperCase()] ?? METHOD_COLORS.GET

// ── Shared Header ─────────────────────────────────────────────────────────────
function Header({ tab, onTab, onReset }) {
  return (
    <header className="flex items-center justify-between px-6 py-3 border-b border-slate-700 bg-slate-900 shrink-0">
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-violet-600">
          <Zap size={18} className="text-white" />
        </div>
        <div>
          <h1 className="text-white font-semibold text-base leading-tight">AgenticDocs</h1>
          <p className="text-slate-400 text-xs">AI-powered API assistant</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex items-center gap-1 bg-slate-800 rounded-lg p-1">
        <button
          onClick={() => onTab('explorer')}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
            tab === 'explorer' ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'
          }`}
        >
          <BookOpen size={13} /> API Explorer
        </button>
        <button
          onClick={() => onTab('chat')}
          className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium transition-colors ${
            tab === 'chat' ? 'bg-violet-600 text-white' : 'text-slate-400 hover:text-white'
          }`}
        >
          <Bot size={13} /> AI Chat
        </button>
      </div>

      <button
        onClick={onReset}
        title="Reset chat"
        className="flex items-center gap-1.5 text-slate-400 hover:text-white text-xs px-3 py-1.5 rounded-md hover:bg-slate-700 transition-colors"
      >
        <RotateCcw size={13} /> New chat
      </button>
    </header>
  )
}

// ── API Explorer ──────────────────────────────────────────────────────────────
function MethodBadge({ method, size = 'sm' }) {
  const c = methodColor(method)
  const padding = size === 'lg' ? 'px-3 py-1 text-sm' : 'px-2 py-0.5 text-xs'
  return (
    <span className={`${c.bg} ${c.text} ${padding} rounded font-mono font-bold uppercase shrink-0`}>
      {method}
    </span>
  )
}

function TryItPanel({ endpoint }) {
  const [body, setBody] = useState('')
  const [pathParams, setPathParams] = useState({})
  const [response, setResponse] = useState(null)
  const [loading, setLoading] = useState(false)

  // Extract path params like {id}
  const paramNames = [...(endpoint.path.matchAll(/\{(\w+)\}/g))].map(m => m[1])

  const buildUrl = () => {
    let url = endpoint.path
    paramNames.forEach(p => { url = url.replace(`{${p}}`, pathParams[p] || p) })
    return url
  }

  const execute = async () => {
    setLoading(true)
    setResponse(null)
    const url = buildUrl()
    try {
      const opts = {
        method: endpoint.httpMethod,
        headers: { 'Content-Type': 'application/json' },
      }
      if (['POST', 'PUT', 'PATCH'].includes(endpoint.httpMethod) && body.trim()) {
        opts.body = body
      }
      const res = await fetch(url, opts)
      const text = await res.text()
      let pretty
      try { pretty = JSON.stringify(JSON.parse(text), null, 2) } catch { pretty = text }
      setResponse({ status: res.status, ok: res.ok, body: pretty })
    } catch (err) {
      setResponse({ status: 0, ok: false, body: err.message })
    } finally {
      setLoading(false)
    }
  }

  const statusColor = response?.ok ? 'text-emerald-400' : 'text-red-400'

  return (
    <div className="mt-3 border border-slate-600 rounded-xl overflow-hidden">
      {/* Path params */}
      {paramNames.length > 0 && (
        <div className="bg-slate-900 px-4 py-3 border-b border-slate-700">
          <p className="text-slate-400 text-xs font-semibold mb-2 uppercase tracking-wide">Path Parameters</p>
          <div className="flex flex-wrap gap-3">
            {paramNames.map(p => (
              <div key={p} className="flex items-center gap-2">
                <label className="text-slate-300 text-xs font-mono">{p}</label>
                <input
                  value={pathParams[p] || ''}
                  onChange={e => setPathParams(prev => ({ ...prev, [p]: e.target.value }))}
                  placeholder={p}
                  className="bg-slate-800 border border-slate-600 rounded px-2 py-1 text-xs text-slate-200 font-mono focus:outline-none focus:border-violet-500 w-32"
                />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Request body */}
      {['POST', 'PUT', 'PATCH'].includes(endpoint.httpMethod) && (
        <div className="bg-slate-900 px-4 py-3 border-b border-slate-700">
          <p className="text-slate-400 text-xs font-semibold mb-2 uppercase tracking-wide">Request Body (JSON)</p>
          <textarea
            value={body}
            onChange={e => setBody(e.target.value)}
            rows={4}
            placeholder={'{\n  \n}'}
            className="w-full bg-slate-950 border border-slate-700 rounded-lg px-3 py-2 text-xs text-green-300 font-mono focus:outline-none focus:border-violet-500 resize-y"
          />
        </div>
      )}

      {/* Execute */}
      <div className="bg-slate-900 px-4 py-3 flex items-center gap-3 border-b border-slate-700">
        <code className="text-slate-300 text-xs font-mono flex-1 truncate">{buildUrl()}</code>
        <button
          onClick={execute}
          disabled={loading}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-violet-600 hover:bg-violet-500 disabled:opacity-50 text-white text-xs rounded-lg font-medium transition-colors"
        >
          {loading ? <Loader2 size={12} className="animate-spin" /> : <Play size={12} />}
          Execute
        </button>
      </div>

      {/* Response */}
      {response && (
        <div className="bg-slate-950 px-4 py-3">
          <p className="text-slate-400 text-xs font-semibold mb-2 uppercase tracking-wide">
            Response — <span className={statusColor}>{response.status || 'Error'}</span>
          </p>
          <pre className="text-xs font-mono text-slate-300 overflow-x-auto whitespace-pre-wrap max-h-60 overflow-y-auto">
            {response.body}
          </pre>
        </div>
      )}
    </div>
  )
}

function EndpointRow({ endpoint, onAskAI }) {
  const [expanded, setExpanded] = useState(false)
  const [tryIt, setTryIt] = useState(false)
  const c = methodColor(endpoint.httpMethod)

  return (
    <div className={`border rounded-xl overflow-hidden transition-all ${expanded ? c.light + ' border' : 'border-slate-700 bg-slate-800/50'}`}>
      {/* Summary row */}
      <button
        onClick={() => setExpanded(v => !v)}
        className="w-full flex items-center gap-3 px-4 py-3 text-left hover:bg-slate-700/30 transition-colors"
      >
        <MethodBadge method={endpoint.httpMethod} />
        <code className="text-slate-200 text-sm font-mono flex-1 truncate">{endpoint.path}</code>
        <span className="text-slate-400 text-xs truncate max-w-xs hidden md:block">{endpoint.description}</span>
        {expanded ? <ChevronUp size={14} className="text-slate-400 shrink-0" /> : <ChevronDown size={14} className="text-slate-400 shrink-0" />}
      </button>

      {/* Expanded detail */}
      {expanded && (
        <div className="px-4 pb-4 border-t border-slate-700/50">
          <div className="mt-3 grid grid-cols-2 gap-2 text-xs text-slate-400">
            <div><span className="text-slate-500">Controller:</span> <span className="text-slate-300 font-mono">{endpoint.controllerName}</span></div>
            <div><span className="text-slate-500">Method:</span> <span className="text-slate-300 font-mono">{endpoint.methodName}()</span></div>
          </div>
          {endpoint.description && endpoint.description !== 'No description provided.' && (
            <p className="mt-2 text-slate-300 text-sm">{endpoint.description}</p>
          )}

          <div className="mt-3 flex items-center gap-2">
            <button
              onClick={() => setTryIt(v => !v)}
              className="flex items-center gap-1.5 px-3 py-1.5 border border-violet-500 text-violet-400 hover:bg-violet-500/10 text-xs rounded-lg font-medium transition-colors"
            >
              <Play size={12} /> {tryIt ? 'Close' : 'Try it out'}
            </button>
            <button
              onClick={() => onAskAI(`Explain the ${endpoint.httpMethod} ${endpoint.path} endpoint and show me a code example`)}
              className="flex items-center gap-1.5 px-3 py-1.5 border border-slate-600 text-slate-400 hover:text-white hover:border-slate-500 text-xs rounded-lg font-medium transition-colors"
            >
              <Bot size={12} /> Ask AI
            </button>
          </div>

          {tryIt && <TryItPanel endpoint={endpoint} />}
        </div>
      )}
    </div>
  )
}

function ApiExplorer({ onAskAI }) {
  const [endpoints, setEndpoints] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')
  const [filterMethod, setFilterMethod] = useState('ALL')

  useEffect(() => {
    fetch(ENDPOINTS_URL)
      .then(r => r.json())
      .then(data => { setEndpoints(data); setLoading(false) })
      .catch(err => { setError(err.message); setLoading(false) })
  }, [])

  const methods = ['ALL', ...new Set(endpoints.map(e => e.httpMethod))]

  const filtered = endpoints.filter(e => {
    const matchMethod = filterMethod === 'ALL' || e.httpMethod === filterMethod
    const q = search.toLowerCase()
    const matchSearch = !q || e.path.toLowerCase().includes(q) ||
      e.description?.toLowerCase().includes(q) ||
      e.controllerName?.toLowerCase().includes(q)
    return matchMethod && matchSearch
  })

  // Group by controller
  const grouped = filtered.reduce((acc, ep) => {
    const key = ep.controllerName || 'Other'
    if (!acc[key]) acc[key] = []
    acc[key].push(ep)
    return acc
  }, {})

  if (loading) return (
    <div className="flex flex-col items-center justify-center flex-1 gap-3 text-slate-400">
      <Loader2 size={32} className="animate-spin text-violet-400" />
      <p className="text-sm">Loading endpoints…</p>
    </div>
  )

  if (error) return (
    <div className="flex flex-col items-center justify-center flex-1 gap-3 text-slate-400 px-6 text-center">
      <p className="text-red-400 text-sm">Failed to load endpoints: {error}</p>
      <p className="text-xs">Make sure the Spring Boot app is running.</p>
    </div>
  )

  return (
    <div className="flex flex-col flex-1 overflow-hidden">
      {/* Toolbar */}
      <div className="shrink-0 px-6 py-4 border-b border-slate-700 bg-slate-900 flex flex-wrap items-center gap-3">
        <div className="flex items-center gap-2 flex-1 min-w-48 bg-slate-800 border border-slate-700 rounded-lg px-3 py-2 focus-within:border-violet-500 transition-colors">
          <Search size={14} className="text-slate-400 shrink-0" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Filter endpoints…"
            className="bg-transparent text-slate-200 text-sm flex-1 outline-none placeholder-slate-500"
          />
        </div>
        <div className="flex gap-1">
          {methods.map(m => {
            const c = m === 'ALL' ? null : methodColor(m)
            const active = filterMethod === m
            return (
              <button
                key={m}
                onClick={() => setFilterMethod(m)}
                className={`px-3 py-1.5 rounded-lg text-xs font-bold transition-colors ${
                  active
                    ? (c ? `${c.bg} text-white` : 'bg-violet-600 text-white')
                    : 'bg-slate-800 text-slate-400 hover:text-white border border-slate-700'
                }`}
              >
                {m}
              </button>
            )
          })}
        </div>
        <span className="text-slate-500 text-xs">{filtered.length} endpoint{filtered.length !== 1 ? 's' : ''}</span>
      </div>

      {/* Endpoint list */}
      <div className="flex-1 overflow-y-auto px-6 py-4">
        {Object.keys(grouped).length === 0 ? (
          <p className="text-slate-500 text-sm text-center mt-10">No endpoints match your filter.</p>
        ) : (
          Object.entries(grouped).map(([controller, eps]) => (
            <div key={controller} className="mb-6">
              <div className="flex items-center gap-2 mb-3">
                <h2 className="text-white font-semibold text-sm">{controller}</h2>
                <span className="text-slate-500 text-xs bg-slate-800 px-2 py-0.5 rounded-full">{eps.length}</span>
              </div>
              <div className="flex flex-col gap-2">
                {eps.map((ep, i) => (
                  <EndpointRow key={i} endpoint={ep} onAskAI={onAskAI} />
                ))}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  )
}

// ── AI Chat ───────────────────────────────────────────────────────────────────
const INITIAL_MESSAGES = []

function SuggestionChips({ onSelect }) {
  return (
    <div className="flex flex-col items-center justify-center flex-1 px-6 py-10 gap-8">
      <div className="text-center">
        <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-violet-600/20 border border-violet-500/30 mx-auto mb-4">
          <Bot size={32} className="text-violet-400" />
        </div>
        <h2 className="text-white text-xl font-semibold mb-2">Ask about your APIs</h2>
        <p className="text-slate-400 text-sm max-w-sm">
          I've indexed all your REST endpoints. Ask me anything — I'll explain them and generate code.
        </p>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 w-full max-w-2xl">
        {SUGGESTIONS.map((s) => (
          <button
            key={s}
            onClick={() => onSelect(s)}
            className="flex items-start gap-3 text-left px-4 py-3 rounded-xl bg-slate-800 border border-slate-700 hover:border-violet-500/60 transition-all group text-sm text-slate-300 hover:text-white"
          >
            <ChevronRight size={14} className="text-violet-400 mt-0.5 shrink-0 group-hover:translate-x-0.5 transition-transform" />
            {s}
          </button>
        ))}
      </div>
    </div>
  )
}

function MessageBubble({ msg }) {
  const isUser = msg.role === 'user'
  return (
    <div className={`flex gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>
      <div className={`shrink-0 flex items-center justify-center w-8 h-8 rounded-full border ${
        isUser ? 'bg-violet-600 border-violet-500' : 'bg-slate-700 border-slate-600'
      }`}>
        {isUser ? <User size={14} className="text-white" /> : <Bot size={14} className="text-violet-300" />}
      </div>
      <div className={`max-w-[78%] rounded-2xl px-4 py-3 text-sm leading-relaxed ${
        isUser
          ? 'bg-violet-600 text-white rounded-tr-sm'
          : 'bg-slate-800 border border-slate-700 text-slate-200 rounded-tl-sm'
      }`}>
        {isUser ? (
          <p>{msg.content}</p>
        ) : (
          <ReactMarkdown
            components={{
              code({ node, children, ...props }) {
                const isInline = node?.position?.start?.line === node?.position?.end?.line
                  && !String(children).includes('\n')
                return isInline
                  ? <code className="bg-slate-900 text-violet-300 px-1.5 py-0.5 rounded text-xs font-mono" {...props}>{children}</code>
                  : (
                    <div className="my-3">
                      <div className="flex items-center gap-2 bg-slate-900 border border-slate-600 rounded-t-lg px-3 py-1.5">
                        <Code2 size={12} className="text-slate-400" />
                        <span className="text-slate-400 text-xs font-mono">code</span>
                      </div>
                      <pre className="bg-slate-950 border border-t-0 border-slate-600 rounded-b-lg p-3 overflow-x-auto">
                        <code className="text-green-300 text-xs font-mono" {...props}>{children}</code>
                      </pre>
                    </div>
                  )
              },
              p({ children }) { return <p className="mb-2 last:mb-0">{children}</p> },
              ul({ children }) { return <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul> },
              ol({ children }) { return <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol> },
              strong({ children }) { return <strong className="text-white font-semibold">{children}</strong> },
            }}
          >
            {msg.content}
          </ReactMarkdown>
        )}
      </div>
    </div>
  )
}

function TypingIndicator() {
  return (
    <div className="flex gap-3">
      <div className="shrink-0 flex items-center justify-center w-8 h-8 rounded-full bg-slate-700 border border-slate-600">
        <Bot size={14} className="text-violet-300" />
      </div>
      <div className="bg-slate-800 border border-slate-700 rounded-2xl rounded-tl-sm px-4 py-3 flex items-center gap-1.5">
        {[0, 150, 300].map((delay) => (
          <span key={delay} className="w-2 h-2 rounded-full bg-violet-400 animate-bounce" style={{ animationDelay: `${delay}ms` }} />
        ))}
      </div>
    </div>
  )
}

function InputBar({ onSend, loading }) {
  const [value, setValue] = useState('')
  const textareaRef = useRef(null)

  const submit = () => {
    const q = value.trim()
    if (!q || loading) return
    setValue('')
    onSend(q)
  }

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submit() }
  }

  useEffect(() => {
    const el = textareaRef.current
    if (!el) return
    el.style.height = 'auto'
    el.style.height = Math.min(el.scrollHeight, 140) + 'px'
  }, [value])

  return (
    <div className="shrink-0 border-t border-slate-700 bg-slate-900 px-4 py-4">
      <div className="max-w-3xl mx-auto flex items-end gap-3">
        <div className="flex-1 flex items-end bg-slate-800 border border-slate-600 focus-within:border-violet-500 rounded-2xl px-4 py-3 transition-colors">
          <textarea
            ref={textareaRef}
            rows={1}
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onKeyDown={handleKey}
            disabled={loading}
            placeholder="Ask about any API endpoint…"
            className="flex-1 bg-transparent text-slate-200 placeholder-slate-500 text-sm resize-none outline-none leading-relaxed max-h-36 overflow-y-auto"
          />
        </div>
        <button
          onClick={submit}
          disabled={loading || !value.trim()}
          className="shrink-0 flex items-center justify-center w-11 h-11 rounded-xl bg-violet-600 hover:bg-violet-500 disabled:opacity-40 disabled:cursor-not-allowed transition-colors"
        >
          <Send size={18} className="text-white" />
        </button>
      </div>
    </div>
  )
}

function AiChat({ pendingQuestion, onPendingConsumed }) {
  const [messages, setMessages] = useState(INITIAL_MESSAGES)
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)
  const showSuggestions = messages.length === 0

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const sendMessage = useCallback(async (question) => {
    setMessages((prev) => [...prev, { role: 'user', content: question }])
    setLoading(true)
    try {
      const res = await fetch(CHAT_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question }),
      })
      if (!res.ok) throw new Error(`HTTP ${res.status}`)
      const data = await res.json()
      setMessages((prev) => [...prev, { role: 'assistant', content: data.answer }])
    } catch (err) {
      setMessages((prev) => [...prev, {
        role: 'assistant',
        content: `**Error:** Could not reach the backend.\n\n\`${err.message}\``,
      }])
    } finally {
      setLoading(false)
    }
  }, [])

  // When Explorer fires "Ask AI", consume the pending question
  useEffect(() => {
    if (pendingQuestion) {
      sendMessage(pendingQuestion)
      onPendingConsumed()
    }
  }, [pendingQuestion, sendMessage, onPendingConsumed])

  return (
    <div className="flex flex-col flex-1 overflow-hidden">
      <main className="flex flex-col flex-1 overflow-y-auto">
        {showSuggestions ? (
          <SuggestionChips onSelect={sendMessage} />
        ) : (
          <div className="max-w-3xl mx-auto w-full px-4 py-6 flex flex-col gap-5">
            {messages.map((msg, i) => <MessageBubble key={i} msg={msg} />)}
            {loading && <TypingIndicator />}
            <div ref={bottomRef} />
          </div>
        )}
      </main>
      <InputBar onSend={sendMessage} loading={loading} />
    </div>
  )
}

// ── App Root ──────────────────────────────────────────────────────────────────
export default function App() {
  const [tab, setTab] = useState('explorer')
  const [pendingQuestion, setPendingQuestion] = useState(null)
  const [resetKey, setResetKey] = useState(0)

  const handleAskAI = (question) => {
    setPendingQuestion(question)
    setTab('chat')
  }

  const handleReset = () => {
    setResetKey(k => k + 1)
    setPendingQuestion(null)
  }

  return (
    <div className="flex flex-col h-screen bg-slate-900 text-white">
      <Header tab={tab} onTab={setTab} onReset={handleReset} />
      {tab === 'explorer'
        ? <ApiExplorer onAskAI={handleAskAI} />
        : <AiChat key={resetKey} pendingQuestion={pendingQuestion} onPendingConsumed={() => setPendingQuestion(null)} />
      }
    </div>
  )
}
