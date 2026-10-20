/**
 * BpmnDiagram
 *
 * Parses a BPMN 2.0 XML string in the browser (no external library),
 * extracts the Diagram Interchange (DI) layout, and renders a zoomable
 * SVG diagram.  Nodes that match active/completed tasks are highlighted.
 *
 * Supports: start/end events, user/service/script/receive tasks, gateways,
 * sub-processes, pools, lanes, and all sequence-flow edges.
 */

import { useMemo, useRef, useState, useCallback, useEffect } from 'react'
import type { ActivityInstance, TaskEntity } from '../types'

// ─── DOM parser helpers ───────────────────────────────────────────────────────

const NS_BPMN   = 'http://www.omg.org/spec/BPMN/20100524/MODEL'
const NS_BPMNDI = 'http://www.omg.org/spec/BPMN/20100524/DI'
const NS_DC     = 'http://www.omg.org/spec/DD/20100524/DC'
const NS_DI     = 'http://www.omg.org/spec/DD/20100524/DI'

function parseXml(xml: string): Document {
  return new DOMParser().parseFromString(xml, 'application/xml')
}

function elsByLocalName(parent: Element | Document, local: string): Element[] {
  const out: Element[] = []
  const all = parent.getElementsByTagNameNS('*', local)
  for (let i = 0; i < all.length; i++) {
    if (all[i].localName === local) out.push(all[i])
  }
  return out
}

function bounds(el: Element): { x: number; y: number; width: number; height: number } | null {
  const b = el.getElementsByTagNameNS(NS_DC, 'Bounds')[0]
  if (!b) return null
  return {
    x:      parseFloat(b.getAttribute('x') ?? '0'),
    y:      parseFloat(b.getAttribute('y') ?? '0'),
    width:  parseFloat(b.getAttribute('width') ?? '0'),
    height: parseFloat(b.getAttribute('height') ?? '0'),
  }
}

function waypoints(el: Element): { x: number; y: number }[] {
  const pts: { x: number; y: number }[] = []
  const wps = el.getElementsByTagNameNS(NS_DI, 'waypoint')
  for (let i = 0; i < wps.length; i++) {
    pts.push({
      x: parseFloat(wps[i].getAttribute('x') ?? '0'),
      y: parseFloat(wps[i].getAttribute('y') ?? '0'),
    })
  }
  return pts
}

// ─── Model types ──────────────────────────────────────────────────────────────

export interface DiagramShape {
  id: string          // bpmnElement id
  diId: string        // BPMNShape id
  type: string        // localName of the BPMN element
  name: string
  x: number; y: number; width: number; height: number
  isHorizontal?: boolean
  isPool?: boolean
  isLane?: boolean
}

export interface DiagramEdge {
  id: string
  diId: string
  name: string
  points: { x: number; y: number }[]
}

export interface DiagramModel {
  shapes: DiagramShape[]
  edges: DiagramEdge[]
  minX: number; minY: number; maxX: number; maxY: number
}

// ─── Classify BPMN element type ───────────────────────────────────────────────

function classifyElement(bpmnType: string): string {
  switch (bpmnType) {
    case 'startEvent':            return 'startEvent'
    case 'endEvent':              return 'endEvent'
    case 'intermediateCatchEvent':
    case 'intermediateThrowEvent': return 'intermediateEvent'
    case 'boundaryEvent':         return 'boundaryEvent'
    case 'userTask':              return 'userTask'
    case 'serviceTask':           return 'serviceTask'
    case 'scriptTask':            return 'scriptTask'
    case 'receiveTask':           return 'receiveTask'
    case 'sendTask':              return 'sendTask'
    case 'manualTask':            return 'manualTask'
    case 'businessRuleTask':      return 'businessRuleTask'
    case 'task':                  return 'task'
    case 'subProcess':
    case 'transaction':           return 'subProcess'
    case 'callActivity':          return 'callActivity'
    case 'exclusiveGateway':      return 'exclusiveGateway'
    case 'parallelGateway':       return 'parallelGateway'
    case 'inclusiveGateway':      return 'inclusiveGateway'
    case 'eventBasedGateway':     return 'eventBasedGateway'
    case 'complexGateway':        return 'complexGateway'
    case 'participant':           return 'participant'
    case 'lane':                  return 'lane'
    default:                      return bpmnType
  }
}

// ─── Parse BPMN XML → DiagramModel ───────────────────────────────────────────

export function parseBpmnDiagram(xml: string): DiagramModel {
  const doc = parseXml(xml)
  const shapes: DiagramShape[] = []
  const edges: DiagramEdge[]   = []

  // Build element-id → localName map from process elements
  const typeMap = new Map<string, string>()
  const nameMap = new Map<string, string>()

  const allEls = doc.querySelectorAll('*')
  allEls.forEach(el => {
    const id = el.getAttribute('id')
    if (id && el.localName) {
      typeMap.set(id, el.localName)
      nameMap.set(id, el.getAttribute('name') ?? '')
    }
  })

  // Collect all BPMNShape elements
  const diShapes = doc.getElementsByTagNameNS(NS_BPMNDI, 'BPMNShape')
  for (let i = 0; i < diShapes.length; i++) {
    const sh = diShapes[i]
    const bpmnEl = sh.getAttribute('bpmnElement') ?? ''
    const b = bounds(sh)
    if (!b) continue
    const type = classifyElement(typeMap.get(bpmnEl) ?? bpmnEl)
    const isPool = type === 'participant'
    const isLane = type === 'lane'
    shapes.push({
      id:   bpmnEl,
      diId: sh.getAttribute('id') ?? '',
      type,
      name: nameMap.get(bpmnEl) ?? '',
      x: b.x, y: b.y, width: b.width, height: b.height,
      isHorizontal: sh.getAttribute('isHorizontal') === 'true',
      isPool,
      isLane,
    })
  }

  // Collect all BPMNEdge elements
  const diEdges = doc.getElementsByTagNameNS(NS_BPMNDI, 'BPMNEdge')
  for (let i = 0; i < diEdges.length; i++) {
    const ed = diEdges[i]
    const bpmnEl = ed.getAttribute('bpmnElement') ?? ''
    const pts = waypoints(ed)
    if (pts.length < 2) continue
    edges.push({
      id:   bpmnEl,
      diId: ed.getAttribute('id') ?? '',
      name: nameMap.get(bpmnEl) ?? '',
      points: pts,
    })
  }

  // Bounding box
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity
  shapes.forEach(s => {
    minX = Math.min(minX, s.x)
    minY = Math.min(minY, s.y)
    maxX = Math.max(maxX, s.x + s.width)
    maxY = Math.max(maxY, s.y + s.height)
  })
  edges.forEach(e => e.points.forEach(p => {
    minX = Math.min(minX, p.x); minY = Math.min(minY, p.y)
    maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y)
  }))

  return { shapes, edges, minX, minY, maxX, maxY }
}

// ─── Task-state colour mapping ────────────────────────────────────────────────

// "active" = activity started but endTime is null (waiting / in progress)
// "completed" = activity has an endTime
function activityFill(active: boolean, completed: boolean): { fill: string; stroke: string; labelColor: string } {
  if (active)    return { fill: '#eff6ff', stroke: '#3b82f6', labelColor: '#1d4ed8' }
  if (completed) return { fill: '#dcfce7', stroke: '#16a34a', labelColor: '#14532d' }
                 return { fill: '#f8fafc', stroke: '#cbd5e1', labelColor: '#475569' }
}

// ─── SVG shape renderers ──────────────────────────────────────────────────────

function wrapText(text: string, maxChars = 14): string[] {
  if (!text) return []
  const words = text.replace(/&#10;/g, ' ').split(/\s+/)
  const lines: string[] = []
  let cur = ''
  words.forEach(w => {
    if ((cur + ' ' + w).trim().length > maxChars) {
      if (cur) lines.push(cur)
      cur = w
    } else {
      cur = (cur + ' ' + w).trim()
    }
  })
  if (cur) lines.push(cur)
  return lines.slice(0, 3)
}

function ShapeLabel({ x, y, width, height, name, color }: {
  x: number; y: number; width: number; height: number
  name: string; color: string
}) {
  if (!name) return null
  const lines = wrapText(name, Math.max(8, Math.floor(width / 7)))
  const lineH = 13
  const totalH = lines.length * lineH
  const startY = y + height / 2 - totalH / 2 + lineH / 2
  return (
    <>
      {lines.map((l, i) => (
        <text key={i}
          x={x + width / 2} y={startY + i * lineH}
          textAnchor="middle" fontSize={11}
          fill={color} style={{ pointerEvents: 'none', userSelect: 'none' }}
          fontFamily="-apple-system, 'Segoe UI', sans-serif"
        >{l}</text>
      ))}
    </>
  )
}

// Arrow marker id
const MARKER_ID = 'bpmn-arrow'

function renderShape(s: DiagramShape, active = false, completed = false, onClick?: (id: string) => void) {
  const { fill, stroke, labelColor } = activityFill(active, completed)
  const isTaskType = ['userTask','serviceTask','scriptTask','receiveTask','sendTask',
                      'manualTask','businessRuleTask','task','subProcess','callActivity'].includes(s.type)
  const isEvent    = s.type.includes('Event')
  const isGateway  = s.type.includes('Gateway')

  const clickable = isTaskType && onClick
  const cursor    = clickable ? 'pointer' : 'default'

  if (s.isPool) {
    return (
      <g key={s.diId}>
        <rect x={s.x} y={s.y} width={s.width} height={s.height}
          fill="#f1f5f9" stroke="#94a3b8" strokeWidth={1.5} rx={3} />
        {s.isHorizontal ? (
          <text x={s.x + 14} y={s.y + s.height / 2}
            textAnchor="middle" fontSize={11} fill="#64748b"
            transform={`rotate(-90, ${s.x + 14}, ${s.y + s.height / 2})`}
            fontFamily="-apple-system, 'Segoe UI', sans-serif"
            style={{ userSelect: 'none' }}
          >{s.name}</text>
        ) : (
          <text x={s.x + s.width / 2} y={s.y + 14}
            textAnchor="middle" fontSize={11} fill="#64748b"
            fontFamily="-apple-system, 'Segoe UI', sans-serif"
            style={{ userSelect: 'none' }}
          >{s.name}</text>
        )}
      </g>
    )
  }

  if (s.isLane) {
    return (
      <g key={s.diId}>
        <rect x={s.x} y={s.y} width={s.width} height={s.height}
          fill="none" stroke="#cbd5e1" strokeWidth={1} rx={0} />
        {s.isHorizontal ? (
          <text x={s.x + 10} y={s.y + s.height / 2}
            textAnchor="middle" fontSize={10} fill="#94a3b8"
            transform={`rotate(-90, ${s.x + 10}, ${s.y + s.height / 2})`}
            fontFamily="-apple-system, 'Segoe UI', sans-serif"
            style={{ userSelect: 'none' }}
          >{s.name}</text>
        ) : (
          <text x={s.x + s.width / 2} y={s.y + 12}
            textAnchor="middle" fontSize={10} fill="#94a3b8"
            fontFamily="-apple-system, 'Segoe UI', sans-serif"
            style={{ userSelect: 'none' }}
          >{s.name}</text>
        )}
      </g>
    )
  }

  if (isEvent) {
    const r  = Math.min(s.width, s.height) / 2
    const cx = s.x + s.width / 2
    const cy = s.y + s.height / 2
    const isStart = s.type === 'startEvent'
    const isEnd   = s.type === 'endEvent'
    return (
      <g key={s.diId} style={{ cursor }}>
        <circle cx={cx} cy={cy} r={r}
          fill={isStart ? '#dcfce7' : isEnd ? '#fee2e2' : '#fef9c3'}
          stroke={isStart ? '#16a34a' : isEnd ? '#dc2626' : '#d97706'}
          strokeWidth={isEnd ? 3 : 1.5}
        />
        {s.name && (
          <text x={cx} y={cy + r + 12} textAnchor="middle" fontSize={10}
            fill="#475569" fontFamily="-apple-system,'Segoe UI',sans-serif"
            style={{ userSelect: 'none' }}
          >{s.name}</text>
        )}
      </g>
    )
  }

  if (isGateway) {
    const hw = s.width / 2
    const hh = s.height / 2
    const cx = s.x + hw
    const cy = s.y + hh
    const pts = `${cx},${s.y} ${s.x + s.width},${cy} ${cx},${s.y + s.height} ${s.x},${cy}`
    const marker = s.type === 'parallelGateway' ? '+' :
                   s.type === 'inclusiveGateway' ? '○' :
                   s.type === 'eventBasedGateway' ? '⬡' : '✕'
    return (
      <g key={s.diId}>
        <polygon points={pts} fill="#fef9c3" stroke="#d97706" strokeWidth={1.5} />
        <text x={cx} y={cy + 5} textAnchor="middle" fontSize={14}
          fill="#92400e" fontFamily="-apple-system,'Segoe UI',sans-serif"
          style={{ userSelect: 'none' }}
        >{marker}</text>
      </g>
    )
  }

  // Task / activity
  const rx = 6
  const hasState = active || completed
  return (
    <g key={s.diId}
      style={{ cursor }}
      onClick={clickable ? () => onClick!(s.id) : undefined}
    >
      <rect x={s.x} y={s.y} width={s.width} height={s.height}
        fill={fill} stroke={stroke} strokeWidth={hasState ? 2 : 1.5}
        rx={rx}
      />
      {/* task type icon */}
      {s.type === 'userTask' && (
        <text x={s.x + 6} y={s.y + 14} fontSize={10} fill={stroke}
          style={{ userSelect: 'none' }}>👤</text>
      )}
      {s.type === 'serviceTask' && (
        <text x={s.x + 6} y={s.y + 14} fontSize={10} fill={stroke}
          style={{ userSelect: 'none' }}>⚙</text>
      )}
      {s.type === 'scriptTask' && (
        <text x={s.x + 6} y={s.y + 14} fontSize={10} fill={stroke}
          style={{ userSelect: 'none' }}>📄</text>
      )}
      {s.type === 'subProcess' && (
        <rect x={s.x + s.width / 2 - 8} y={s.y + s.height - 14}
          width={16} height={10} rx={2}
          fill={fill} stroke={stroke} strokeWidth={1} />
      )}
      <ShapeLabel x={s.x} y={s.y} width={s.width} height={s.height}
        name={s.name} color={labelColor} />
    </g>
  )
}

function renderEdge(e: DiagramEdge) {
  if (e.points.length < 2) return null
  const d = e.points.map((p, i) => `${i === 0 ? 'M' : 'L'}${p.x},${p.y}`).join(' ')
  const last = e.points[e.points.length - 1]
  const prev = e.points[e.points.length - 2]
  return (
    <g key={e.diId}>
      <path d={d} fill="none" stroke="#94a3b8" strokeWidth={1.5}
        markerEnd={`url(#${MARKER_ID})`} />
      {e.name && (
        <text x={(prev.x + last.x) / 2} y={(prev.y + last.y) / 2 - 4}
          textAnchor="middle" fontSize={9} fill="#94a3b8"
          style={{ userSelect: 'none' }}
        >{e.name}</text>
      )}
    </g>
  )
}

// ─── Main component ───────────────────────────────────────────────────────────

interface Props {
  bpmnXml: string
  activities: ActivityInstance[]   // all history activity instances — drives diagram colouring
  tasks: TaskEntity[]              // user tasks only — drives Claim/Complete panel
  onTaskClick?: (taskId: string) => void
}

export default function BpmnDiagram({ bpmnXml, activities, tasks, onTaskClick }: Props) {
  const svgRef     = useRef<SVGSVGElement>(null)
  const [zoom, setZoom]       = useState(1)
  const [pan, setPan]         = useState({ x: 0, y: 0 })
  const dragging = useRef<{ sx: number; sy: number; px: number; py: number } | null>(null)

  const model = useMemo(() => {
    try { return parseBpmnDiagram(bpmnXml) } catch { return null }
  }, [bpmnXml])

  // activityId → ActivityInstance — exact match to DiagramShape.id, covers all node types
  const activityById = useMemo(() => {
    const m = new Map<string, ActivityInstance>()
    // If the same activityId appears multiple times (e.g. loop), keep the most recent
    activities.forEach(a => {
      const existing = m.get(a.activityId)
      if (!existing || a.startTime > existing.startTime) m.set(a.activityId, a)
    })
    return m
  }, [activities])

  // taskDefinitionKey → TaskEntity — for onTaskClick (user tasks only)
  const taskById = useMemo(() => {
    const m = new Map<string, TaskEntity>()
    tasks.forEach(t => { if (t.taskDefinitionKey) m.set(t.taskDefinitionKey, t) })
    return m
  }, [tasks])

  // Fit diagram to viewport on first load
  useEffect(() => {
    if (!model || !svgRef.current) return
    const w = svgRef.current.clientWidth  || 800
    const h = svgRef.current.clientHeight || 500
    const dw = model.maxX - model.minX
    const dh = model.maxY - model.minY
    if (dw <= 0 || dh <= 0) return
    const scale = Math.min(0.95 * w / dw, 0.95 * h / dh, 1.5)
    setZoom(scale)
    setPan({
      x: (w - dw * scale) / 2 - model.minX * scale,
      y: (h - dh * scale) / 2 - model.minY * scale,
    })
  }, [model])

  const onWheel = useCallback((e: React.WheelEvent) => {
    e.preventDefault()
    setZoom(z => Math.max(0.15, Math.min(4, z * (e.deltaY < 0 ? 1.1 : 0.9))))
  }, [])

  const onMouseDown = useCallback((e: React.MouseEvent) => {
    if (e.button !== 0) return
    dragging.current = { sx: e.clientX, sy: e.clientY, px: pan.x, py: pan.y }
  }, [pan])

  const onMouseMove = useCallback((e: React.MouseEvent) => {
    if (!dragging.current) return
    setPan({
      x: dragging.current.px + (e.clientX - dragging.current.sx),
      y: dragging.current.py + (e.clientY - dragging.current.sy),
    })
  }, [])

  const onMouseUp = useCallback(() => { dragging.current = null }, [])

  const resetView = () => {
    if (!model || !svgRef.current) return
    const w = svgRef.current.clientWidth  || 800
    const h = svgRef.current.clientHeight || 500
    const dw = model.maxX - model.minX
    const dh = model.maxY - model.minY
    const scale = Math.min(0.95 * w / dw, 0.95 * h / dh, 1.5)
    setZoom(scale)
    setPan({
      x: (w - dw * scale) / 2 - model.minX * scale,
      y: (h - dh * scale) / 2 - model.minY * scale,
    })
  }

  if (!model) {
    return <div className="empty-state"><p>Could not render diagram.</p></div>
  }

  // Separate pools/lanes from flow nodes
  const pools  = model.shapes.filter(s => s.isPool)
  const lanes  = model.shapes.filter(s => s.isLane)
  const nodes  = model.shapes.filter(s => !s.isPool && !s.isLane)

  return (
    <div className="bpmn-diagram-wrap">
      {/* toolbar */}
      <div className="bpmn-diagram-toolbar">
        <button className="bpmn-zoom-btn" onClick={() => setZoom(z => Math.min(z * 1.2, 4))}>+</button>
        <button className="bpmn-zoom-btn" onClick={() => setZoom(z => Math.max(z * 0.8, 0.15))}>−</button>
        <button className="bpmn-zoom-btn" onClick={resetView} title="Fit to screen">⊡</button>
        <span className="bpmn-zoom-label">{Math.round(zoom * 100)}%</span>
        {/* legend */}
        <span className="bpmn-legend">
          <span className="bpmn-legend-item" style={{ background: '#eff6ff', borderColor: '#3b82f6' }}>Active</span>
          <span className="bpmn-legend-item" style={{ background: '#dcfce7', borderColor: '#16a34a' }}>Completed</span>
          <span className="bpmn-legend-item" style={{ background: '#f8fafc', borderColor: '#cbd5e1' }}>Not reached</span>
        </span>
      </div>

      <svg
        ref={svgRef}
        className="bpmn-svg"
        onWheel={onWheel}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseUp={onMouseUp}
        onMouseLeave={onMouseUp}
        style={{ cursor: dragging.current ? 'grabbing' : 'grab' }}
      >
        <defs>
          <marker id={MARKER_ID} markerWidth={10} markerHeight={7}
            refX={9} refY={3.5} orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="#94a3b8" />
          </marker>
        </defs>

        <g transform={`translate(${pan.x},${pan.y}) scale(${zoom})`}>
          {/* Pools first (background) */}
          {pools.map(s => renderShape(s))}
          {/* Lanes */}
          {lanes.map(s => renderShape(s))}
          {/* Edges below nodes */}
          {model.edges.map(e => renderEdge(e))}
          {/* Nodes */}
          {nodes.map(s => {
            // Colour from activity history (activityId === DiagramShape.id — exact match)
            const activity = activityById.get(s.id)
            const active    = !!activity && activity.endTime === null
            const completed = !!activity && activity.endTime !== null
            // For click handler, look up the corresponding user task (if any)
            const userTask = taskById.get(s.id)
            return renderShape(s, active, completed, onTaskClick && userTask?.id ? () => {
              onTaskClick(userTask.id!)
            } : undefined)
          })}
        </g>
      </svg>
    </div>
  )
}
