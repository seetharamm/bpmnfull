// ── Process Definitions ───────────────────────────────────────────────────────

export interface ProcessDefinitionDto {
  id: string
  key: string
  version: number
  name: string | null
  description: string | null
  suspended: boolean
  active: boolean
  resourceName: string | null
  deploymentId: string | null
  deployedBy: string | null
  deployMessage: string | null
  createdAt: string
  updatedAt: string
  bpmnXml?: string
  runningInstanceCount?: number
}

export interface ProcessVersionSummaryDto {
  key: string
  name: string | null
  latestVersion: number
  activeVersion: number
  suspended: boolean
  totalVersions: number
  totalRunningInstances: number
  versions: ProcessDefinitionDto[]
}

export interface DeployProcessRequest {
  resourceName?: string
  bpmnXml: string
  deployedBy?: string
  deployMessage?: string
}

// ── Process Instances ─────────────────────────────────────────────────────────

export interface ProcessInstanceResult {
  id: string
  businessKey: string | null
  processDefinitionId: string
  processDefinitionKey: string
  state: 'RUNNING' | 'COMPLETED' | 'SUSPENDED' | 'TERMINATED'
  startTime: string
  endTime: string | null
}

// ── Task Detail ───────────────────────────────────────────────────────────────

export interface TaskVariableDto {
  name: string
  type: string
  value: string
  updatedAt: string
}

export interface TaskIoMappingDto {
  source: string | null
  target: string | null
  transformation: string | null
}

export interface TaskDetailDto {
  id: string
  name: string | null
  description: string | null
  taskDefinitionKey: string | null
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey: string | null
  executionId: string
  assignee: string | null
  owner: string | null
  candidateUsers: string | null
  candidateGroups: string | null
  state: string
  priority: number
  formKey: string | null
  dueDate: string | null
  createdAt: string
  updatedAt: string
  completedAt: string | null
  processVariables: TaskVariableDto[]
  localVariables: TaskVariableDto[]
  inputs: TaskIoMappingDto[]
  outputs: TaskIoMappingDto[]
}

// ── Activity Instances (history — all node types) ────────────────────────────

export interface ActivityInstance {
  id: string
  activityId: string        // = BPMN element id (matches DiagramShape.id)
  activityName: string | null
  activityType: string      // e.g. "USER_TASK", "SERVICE_TASK", "PARALLEL_GATEWAY"
  executionId: string
  processInstanceId: string
  processDefinitionId: string
  processDefinitionKey: string | null
  startTime: string
  endTime: string | null    // null = still active / in wait state
  durationMs: number | null
  assignee: string | null
  taskId: string | null
  createdAt: string
}

// ── Tasks ─────────────────────────────────────────────────────────────────────

export interface TaskEntity {
  id: string | null
  name: string | null
  description: string | null
  taskDefinitionKey: string | null
  processInstanceId: string
  processDefinitionId: string | null
  executionId: string | null
  assignee: string | null
  owner: string | null
  candidateGroups: string | null
  state: 'CREATED' | 'ASSIGNED' | 'COMPLETED' | 'DELETED' | 'PENDING'
  priority: number
  formKey: string | null
  activityType: string | null   // e.g. "USER_TASK", "SERVICE_TASK", "SCRIPT_TASK"
  dueDate: string | null
  createdAt: string | null
  updatedAt: string | null
  completedAt: string | null
}

// ── Identity ──────────────────────────────────────────────────────────────────

export interface UserDto {
  id: string
  username: string
  email: string
  firstName: string | null
  lastName: string | null
  displayName: string | null
  enabled: boolean
  locked: boolean
  roles: string[]
  groups: string[]
  effectiveRoles: string[]
  createdAt: string
  updatedAt: string
}

export interface RoleDto {
  id: string
  name: string
  description: string | null
  createdAt: string
}

export interface GroupDto {
  id: string
  name: string
  displayName: string | null
  type: string | null
  roles: string[]
  createdAt: string
}

export interface MembershipDto {
  id: string
  userId: string
  username: string
  groupId: string
  groupName: string
  addedBy: string | null
  joinedAt: string
}

export interface CreateUserRequest {
  username: string
  email: string
  password: string
  firstName?: string
  lastName?: string
  displayName?: string
  roleNames?: string[]
  groupNames?: string[]
}

export interface UpdateUserRequest {
  email?: string
  firstName?: string
  lastName?: string
  displayName?: string
  enabled?: boolean
  locked?: boolean
  newPassword?: string
}

export interface CreateRoleRequest {
  name: string
  description?: string
}

export interface CreateGroupRequest {
  name: string
  displayName?: string
  type?: string
  roleNames?: string[]
}

// ── Pagination ────────────────────────────────────────────────────────────────

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// ── Errors ────────────────────────────────────────────────────────────────────

export interface ApiError {
  timestamp: string
  status: number
  error: string
  message: string
}
