import { createContext, useContext } from 'react'

interface SidebarCtx {
  open: boolean
  toggle: () => void
}

const SidebarContext = createContext<SidebarCtx>({ open: true, toggle: () => {} })

export const useSidebar = () => useContext(SidebarContext)
export default SidebarContext
