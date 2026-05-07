import { useState } from 'react'
import { Copy, Check, ChevronDown, ChevronUp, AlertTriangle } from 'lucide-react'

/** Layer badge colour map */
const LAYER_STYLES = {
  CONTROLLER: 'bg-violet-500/15 border-violet-500/30 text-violet-300',
  SERVICE:    'bg-blue-500/15   border-blue-500/30   text-blue-300',
  REPOSITORY: 'bg-amber-500/15  border-amber-500/30  text-amber-300',
  COMPONENT:  'bg-slate-500/15  border-slate-500/30  text-slate-300',
}

function CopyBtn({ text }) {
  const [copied, setCopied] = useState(false)
  const copy = () => {
    navigator.clipboard.writeText(text)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }
  return (
    <button onClick={copy} className="flex items-center gap-1 text-[10px] text-slate-500 hover:text-white transition-colors px-1.5 py-0.5 rounded hover:bg-white/10">
      {copied ? <Check size={9} className="text-emerald-400" /> : <Copy size={9} />}
      {copied ? 'Copied' : 'Copy'}
    </button>
  )
}

function JsonPanel({ label, value, accent }) {
  if (!value || value === 'null') return null
  return (
    <div className="mt-2">
      <p className={`text-[10px] font-semibold uppercase tracking-widest mb-1 ${accent}`}>{label}</p>
      <div className="rounded-lg overflow-hidden border border-white/6">
        <div className="flex items-center justify-between bg-[#080a10] px-3 py-1.5 border-b border-white/6">
          <span className="text-[10px] text-slate-600 font-mono">json</span>
          <CopyBtn text={value} />
        </div>
        <pre className="bg-[#0a0c14] px-3 py-2.5 text-[11px] font-mono text-slate-300 overflow-x-auto whitespace-pre-wrap max-h-40 overflow-y-auto leading-relaxed">
          {value}
        </pre>
      </div>
    </div>
  )
}

/**
 * A single card in the Flow Tracer vertical diagram.
 * Represents one intercepted method call (one TraceEvent).
 *
 * @param {{ step: object, isLast: boolean }} props
 */
export default function FlowStepCard({ step, isLast }) {
  const [expanded, setExpanded] = useState(true)

  const isError  = step.status === 'ERROR'
  const layerCls = LAYER_STYLES[step.layer] ?? LAYER_STYLES.COMPONENT

  const durationColor =
    step.durationMs < 30  ? 'text-emerald-400' :
    step.durationMs < 100 ? 'text-amber-400'   :
                            'text-red-400'

  return (
    <div className="flex flex-col items-center w-full">
      {/* Card */}
      <div className={`w-full rounded-xl border transition-all duration-300 animate-fadeIn overflow-hidden ${
        isError
          ? 'border-red-500/40 bg-[#1a0d0d]'
          : 'border-white/8 bg-[#13151f]'
      }`}>
        {/* Header row */}
        <button
          onClick={() => setExpanded((v) => !v)}
          className="w-full flex items-center gap-3 px-4 py-3 text-left"
        >
          {/* Step index bubble */}
          <div className={`shrink-0 flex items-center justify-center w-6 h-6 rounded-full text-[10px] font-bold ${
            isError ? 'bg-red-500/20 text-red-400 border border-red-500/30' : 'bg-violet-500/20 text-violet-400 border border-violet-500/30'
          }`}>
            {step.stepIndex + 1}
          </div>

          {/* Layer badge */}
          <span className={`shrink-0 px-2 py-0.5 rounded-md border text-[10px] font-bold uppercase tracking-wide ${layerCls}`}>
            {step.layer}
          </span>

          {/* Class.method() */}
          <code className="flex-1 text-xs font-mono text-slate-200 truncate">
            {step.className}<span className="text-slate-500">.</span>{step.methodName}<span className="text-slate-500">()</span>
          </code>

          {/* Duration */}
          <span className={`shrink-0 text-[10px] font-mono font-semibold ${durationColor}`}>
            {step.durationMs}ms
          </span>

          {/* Error icon */}
          {isError && <AlertTriangle size={13} className="shrink-0 text-red-400" />}

          {/* Expand toggle */}
          <div className="shrink-0 text-slate-600">
            {expanded ? <ChevronUp size={13} /> : <ChevronDown size={13} />}
          </div>
        </button>

        {/* Expanded body */}
        {expanded && (
          <div className="px-4 pb-4 border-t border-white/5">
            {isError ? (
              <div className="mt-3">
                <p className="text-[10px] font-semibold uppercase tracking-widest text-red-400 mb-1">Error</p>
                <pre className="bg-[#0f0808] border border-red-500/20 rounded-lg px-3 py-2.5 text-[11px] font-mono text-red-300 overflow-x-auto whitespace-pre-wrap max-h-48 overflow-y-auto leading-relaxed">
                  {step.errorMessage}
                </pre>
              </div>
            ) : (
              <>
                <JsonPanel label="Input"  value={step.inputJson}  accent="text-sky-400" />
                <JsonPanel label="Output" value={step.outputJson} accent="text-emerald-400" />
              </>
            )}
          </div>
        )}
      </div>

      {/* Dashed connector line to next step */}
      {!isLast && (
        <div className="flex flex-col items-center py-1">
          <div className="w-px h-5 border-l-2 border-dashed border-white/15" />
          <div className="w-1.5 h-1.5 rounded-full bg-white/15" />
          <div className="w-px h-5 border-l-2 border-dashed border-white/15" />
        </div>
      )}
    </div>
  )
}
