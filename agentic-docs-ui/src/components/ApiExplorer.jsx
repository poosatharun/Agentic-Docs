import { useState } from 'react'
import { Search, Loader2, BookOpen, Layers, AlertCircle } from 'lucide-react'
import EndpointRow  from './EndpointRow'
import { methodColor } from '../constants/methodColors'
import { useEndpoints } from '../hooks/useEndpoints'

/**
 * "API Explorer" tab — shows all discovered REST endpoints grouped by controller.
 */
export default function ApiExplorer({ onAskAI }) {
  const { endpoints, loading, error } = useEndpoints()
  const [search,       setSearch]       = useState('')
  const [filterMethod, setFilterMethod] = useState('ALL')

  if (loading) return (
    <div className="flex flex-col items-center justify-center flex-1 gap-4 text-slate-400 bg-[#0f1117]">
      <div className="relative">
        <div className="w-12 h-12 rounded-full border-2 border-violet-500/20 border-t-violet-500 animate-spin" />
        <div className="absolute inset-0 flex items-center justify-center">
          <Layers size={16} className="text-violet-400" />
        </div>
      </div>
      <div className="text-center">
        <p className="text-sm text-white font-medium">Loading endpoints</p>
        <p className="text-xs text-slate-600 mt-1">Scanning your Spring Boot application…</p>
      </div>
    </div>
  )

  if (error) return (
    <div className="flex flex-col items-center justify-center flex-1 gap-4 px-6 text-center bg-[#0f1117]">
      <div className="flex items-center justify-center w-12 h-12 rounded-2xl bg-red-500/10 border border-red-500/20">
        <AlertCircle size={22} className="text-red-400" />
      </div>
      <div>
        <p className="text-white font-semibold text-sm">Failed to load endpoints</p>
        <p className="text-red-400/80 text-xs mt-1">{error}</p>
        <p className="text-slate-600 text-xs mt-3">Make sure your Spring Boot app is running.</p>
      </div>
    </div>
  )

  const methods  = ['ALL', ...new Set(endpoints.map((e) => e.httpMethod))]
  const filtered = endpoints.filter((e) => {
    const matchMethod = filterMethod === 'ALL' || e.httpMethod === filterMethod
    const q           = search.toLowerCase()
    const matchSearch = !q
      || e.path.toLowerCase().includes(q)
      || e.description?.toLowerCase().includes(q)
      || e.controllerName?.toLowerCase().includes(q)
    return matchMethod && matchSearch
  })

  const grouped = filtered.reduce((acc, ep) => {
    const key = ep.controllerName || 'Other'
    if (!acc[key]) acc[key] = []
    acc[key].push(ep)
    return acc
  }, {})

  return (
    <div className="flex flex-col flex-1 overflow-hidden bg-[#0f1117]">
      {/* Page header */}
      <div className="shrink-0 px-6 py-4 border-b border-white/5 bg-[#13151f]/60 backdrop-blur-sm">
        <div className="flex items-center gap-3 mb-4">
          <div className="flex items-center justify-center w-8 h-8 rounded-lg bg-violet-600/20 border border-violet-500/20">
            <BookOpen size={15} className="text-violet-400" />
          </div>
          <div className="flex-1">
            <h2 className="text-sm font-semibold text-white">API Explorer</h2>
            <p className="text-[11px] text-slate-500">{endpoints.length} endpoints discovered</p>
          </div>
          {/* Stats badges */}
          <div className="flex items-center gap-2">
            {['GET','POST','PUT','DELETE'].filter(m => endpoints.some(e => e.httpMethod === m)).map(m => {
              const c = methodColor(m)
              const count = endpoints.filter(e => e.httpMethod === m).length
              return (
                <div key={m} className={`hidden md:flex items-center gap-1 px-2 py-0.5 rounded-md ${c.bg} bg-opacity-20`}>
                  <span className={`text-[10px] font-bold ${c.text}`}>{m}</span>
                  <span className="text-[10px] text-slate-500">{count}</span>
                </div>
              )
            })}
          </div>
        </div>

        {/* Search + filter row */}
        <div className="flex flex-wrap items-center gap-2">
          <div className="flex items-center gap-2 flex-1 min-w-48 bg-[#1a1d2e] border border-white/8 rounded-xl px-3 py-2 focus-within:border-violet-500/50 transition-colors">
            <Search size={13} className="text-slate-600 shrink-0" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search by path, description, or controller…"
              className="bg-transparent text-slate-200 text-xs flex-1 outline-none placeholder-slate-600"
            />
            {search && (
              <button onClick={() => setSearch('')} className="text-slate-600 hover:text-white text-xs">×</button>
            )}
          </div>

          <div className="flex gap-1 flex-wrap">
            {methods.map((m) => {
              const c      = m === 'ALL' ? null : methodColor(m)
              const active = filterMethod === m
              return (
                <button
                  key={m}
                  onClick={() => setFilterMethod(m)}
                  className={`px-2.5 py-1.5 rounded-lg text-[11px] font-bold transition-all duration-150 ${
                    active
                      ? (c ? `${c.bg} text-white shadow-sm` : 'bg-violet-600 text-white shadow-sm shadow-violet-900/40')
                      : 'bg-[#1a1d2e] text-slate-500 hover:text-white border border-white/6 hover:border-white/15'
                  }`}
                >
                  {m}
                </button>
              )
            })}
          </div>

          <span className="text-slate-600 text-[11px] shrink-0">
            {filtered.length} result{filtered.length !== 1 ? 's' : ''}
          </span>
        </div>
      </div>

      {/* Endpoint list */}
      <div className="flex-1 overflow-y-auto px-6 py-5 scrollbar-thin">
        {Object.keys(grouped).length === 0 ? (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <Search size={28} className="text-slate-700 mb-3" />
            <p className="text-slate-500 text-sm font-medium">No endpoints match your filter</p>
            <p className="text-slate-700 text-xs mt-1">Try adjusting your search or method filter</p>
          </div>
        ) : (
          Object.entries(grouped).map(([controller, eps]) => (
            <div key={controller} className="mb-7">
              <div className="flex items-center gap-2 mb-3">
                <div className="flex items-center gap-2 flex-1">
                  <div className="w-1 h-4 rounded-full bg-gradient-to-b from-violet-500 to-purple-700" />
                  <h2 className="text-white font-semibold text-xs tracking-wide">{controller}</h2>
                  <span className="text-slate-600 text-[10px] bg-white/5 border border-white/8 px-1.5 py-0.5 rounded-full font-mono">
                    {eps.length}
                  </span>
                </div>
                <div className="flex-1 h-px bg-white/5" />
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
