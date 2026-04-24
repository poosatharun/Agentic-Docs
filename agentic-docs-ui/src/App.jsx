import { useState, useRef, useEffect, useCallback } from 'react'
import ReactMarkdown from 'react-markdown'
import { Send, Bot, User, Zap, Code2, BookOpen, ChevronRight, RotateCcw } from 'lucide-react'

const API_URL = '/agentic-docs/api/chat'

const SUGGESTIONS = [
  'How do I cancel a subscription with a partial refund?',
  'Show me how to process a payment with the API',
  'What endpoints handle refund calculation?',
  'How do I upgrade a user\'s subscription plan?',
  'Generate Java code to get subscription details',
  'What request body does the terminate endpoint need?',
]

// ── Sub-components ────────────────────────────────────────────────────────────

function Header({ onReset }) {
  return (
    <header className="flex items-center justify-between px-6 py-4 border-b border-slate-700 bg-slate-900 shrink-0">
      <div className="flex items-center gap-3">
        <div className="flex items-center justify-center w-9 h-9 rounded-lg bg-violet-600">
          <Zap size={18} className="text-white" />
        </div>
        <div>
          <h1 className="text-white font-semibold text-base leading-tight">AgenticDocs</h1>
          <p className="text-slate-400 text-xs">AI-powered API assistant</p>
        </div>
      </div>
      <button
        onClick={onReset}
        title="New conversation"
        className="flex items-center gap-1.5 text-slate-400 hover:text-white text-xs px-3 py-1.5 rounded-md hover:bg-slate-700 transition-colors"
      >
        <RotateCcw size={13} />
        New chat
      </button>
    </header>
  )
}

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
            className="flex items-start gap-3 text-left px-4 py-3 rounded-xl bg-slate-800 border border-slate-700 hover:border-violet-500/60 hover:bg-slate-750 transition-all group text-sm text-slate-300 hover:text-white"
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
      {/* Avatar */}
      <div className={`shrink-0 flex items-center justify-center w-8 h-8 rounded-full border ${
        isUser
          ? 'bg-violet-600 border-violet-500'
          : 'bg-slate-700 border-slate-600'
      }`}>
        {isUser
          ? <User size={14} className="text-white" />
          : <Bot size={14} className="text-violet-300" />
        }
      </div>

      {/* Bubble */}
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
                  && !String(children).includes('\n');
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
              p({ children }) {
                return <p className="mb-2 last:mb-0">{children}</p>
              },
              ul({ children }) {
                return <ul className="list-disc list-inside mb-2 space-y-1">{children}</ul>
              },
              ol({ children }) {
                return <ol className="list-decimal list-inside mb-2 space-y-1">{children}</ol>
              },
              strong({ children }) {
                return <strong className="text-white font-semibold">{children}</strong>
              },
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
          <span
            key={delay}
            className="w-2 h-2 rounded-full bg-violet-400 animate-bounce"
            style={{ animationDelay: `${delay}ms` }}
          />
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
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      submit()
    }
  }

  // Auto-resize textarea
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
          <Send size={16} className="text-white" />
        </button>
      </div>
      <p className="text-center text-slate-600 text-xs mt-2">
        Press <kbd className="bg-slate-700 text-slate-400 px-1 rounded text-xs">Enter</kbd> to send · <kbd className="bg-slate-700 text-slate-400 px-1 rounded text-xs">Shift+Enter</kbd> for new line
      </p>
    </div>
  )
}

// ── Main App ──────────────────────────────────────────────────────────────────

const INITIAL_MESSAGES = [
  {
    role: 'assistant',
    content: "Hi! I'm your API assistant. I've indexed all the REST endpoints in this application.\n\nAsk me anything — I can explain endpoints, describe request/response shapes, and generate Java or React code snippets for you.",
  },
]

export default function App() {
  const [messages, setMessages] = useState(INITIAL_MESSAGES)
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)
  const showSuggestions = messages.length === 1

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const sendMessage = useCallback(async (question) => {
    setMessages((prev) => [...prev, { role: 'user', content: question }])
    setLoading(true)

    try {
      const res = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ question }),
      })

      if (!res.ok) throw new Error(`HTTP ${res.status}`)

      const data = await res.json()
      setMessages((prev) => [...prev, { role: 'assistant', content: data.answer }])
    } catch (err) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: `**Error:** Could not reach the backend.\n\nMake sure the Spring Boot app is running on \`localhost:8080\` with \`agentic.docs.enabled=true\`.\n\n\`${err.message}\``,
        },
      ])
    } finally {
      setLoading(false)
    }
  }, [])

  const reset = () => setMessages(INITIAL_MESSAGES)

  return (
    <div className="flex flex-col h-screen bg-slate-900 text-white">
      <Header onReset={reset} />

      <main className="flex flex-col flex-1 overflow-y-auto">
        {showSuggestions ? (
          <SuggestionChips onSelect={sendMessage} />
        ) : (
          <div className="max-w-3xl mx-auto px-4 py-6 flex flex-col gap-5">
            {messages.map((msg, i) => (
              <MessageBubble key={i} msg={msg} />
            ))}
            {loading && <TypingIndicator />}
            <div ref={bottomRef} />
          </div>
        )}
      </main>

      <InputBar onSend={sendMessage} loading={loading} />
    </div>
  )
}
