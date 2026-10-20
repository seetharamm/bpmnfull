type State = 'RUNNING' | 'COMPLETED' | 'SUSPENDED' | 'TERMINATED' |
             'CREATED' | 'ASSIGNED' | 'DELETED' |
             string

const map: Record<string, string> = {
  RUNNING:    'badge-blue',
  COMPLETED:  'badge-green',
  SUSPENDED:  'badge-yellow',
  TERMINATED: 'badge-red',
  CREATED:    'badge-blue',
  ASSIGNED:   'badge-purple',
  DELETED:    'badge-gray',
  ACTIVE:     'badge-green',
}

export default function StateBadge({ state }: { state: State }) {
  const cls = map[state] ?? 'badge-gray'
  return <span className={`badge ${cls}`}>{state}</span>
}
