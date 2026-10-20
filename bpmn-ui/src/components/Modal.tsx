import { useSidebar } from '../SidebarContext'

interface Props {
  title: string
  onClose: () => void
  children: React.ReactNode
  footer?: React.ReactNode
  wide?: boolean
}

export default function Modal({ title, onClose, children, footer, wide }: Props) {
  const { open } = useSidebar()
  return (
    <div className={`modal-overlay${open ? '' : ' sidebar-collapsed'}`} onMouseDown={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className="modal" style={wide ? { maxWidth: 860 } : undefined}>
        <div className="modal-header">
          <span className="modal-title">{title}</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        {children}
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  )
}
