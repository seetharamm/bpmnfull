import axios from 'axios'
import type {
  ProcessDefinitionDto,
  ProcessVersionSummaryDto,
  DeployProcessRequest,
  ProcessInstanceResult,
  ActivityInstance,
  TaskDetailDto,
  TaskEntity,
  UserDto,
  RoleDto,
  GroupDto,
  MembershipDto,
  CreateUserRequest,
  UpdateUserRequest,
  CreateRoleRequest,
  CreateGroupRequest,
  Page,
} from './types'

const api = axios.create({ baseURL: '/api' })

// surfaced globally for error toasting
api.interceptors.response.use(
  r => r,
  err => {
    const msg: string =
      err?.response?.data?.message ??
      err?.response?.data?.error ??
      err.message ??
      'Unknown error'
    return Promise.reject(new Error(msg))
  },
)

// ── Definitions ───────────────────────────────────────────────────────────────

export const definitionsApi = {
  list: (q?: string) =>
    api.get<ProcessDefinitionDto[]>('/definitions', { params: q ? { q } : {} }).then(r => r.data),

  getActive: (key: string) =>
    api.get<ProcessDefinitionDto>(`/definitions/${key}`).then(r => r.data),

  getById: (id: string) =>
    api.get<ProcessDefinitionDto>(`/definitions/by-id/${id}`).then(r => r.data),

  getVersionHistory: (key: string) =>
    api.get<ProcessVersionSummaryDto>(`/definitions/${key}/versions`).then(r => r.data),

  getVersion: (key: string, version: number) =>
    api.get<ProcessDefinitionDto>(`/definitions/${key}/versions/${version}`).then(r => r.data),

  deploy: (req: DeployProcessRequest) =>
    api.post<ProcessDefinitionDto>('/definitions/deploy', req).then(r => r.data),

  setActive: (key: string, version: number) =>
    api.post<ProcessDefinitionDto>(`/definitions/${key}/versions/${version}/activate`).then(r => r.data),

  suspendAll: (key: string) =>
    api.post<void>(`/definitions/${key}/suspend`).then(() => undefined),

  activateAll: (key: string) =>
    api.post<void>(`/definitions/${key}/activate`).then(() => undefined),

  suspendVersion: (key: string, version: number) =>
    api.post<void>(`/definitions/${key}/versions/${version}/suspend`).then(() => undefined),

  resumeVersion: (key: string, version: number) =>
    api.post<void>(`/definitions/${key}/versions/${version}/resume`).then(() => undefined),

  deleteVersion: (key: string, version: number) =>
    api.delete<void>(`/definitions/${key}/versions/${version}`).then(() => undefined),

  deleteAll: (key: string) =>
    api.delete<void>(`/definitions/${key}`).then(() => undefined),

  startVersion: (key: string, version: number, businessKey?: string, variables?: Record<string, unknown>) =>
    api
      .post<ProcessInstanceResult>(`/definitions/${key}/versions/${version}/start`, { businessKey, variables })
      .then(r => r.data),

  bpmnUrl: (key: string, version: number) => `/api/definitions/${key}/versions/${version}/bpmn`,
}

// ── Instances ─────────────────────────────────────────────────────────────────

export const instancesApi = {
  list: (state?: string) =>
    api.get<ProcessInstanceResult[]>('/processes', { params: state ? { state } : {} }).then(r => r.data),

  start: (key: string, businessKey?: string, variables?: Record<string, unknown>) =>
    api.post<ProcessInstanceResult>(`/processes/${key}/start`, { businessKey, variables }).then(r => r.data),

  get: (id: string) =>
    api.get<ProcessInstanceResult>(`/processes/${id}`).then(r => r.data),

  suspend: (id: string) =>
    api.post<void>(`/processes/${id}/suspend`).then(() => undefined),

  activate: (id: string) =>
    api.post<void>(`/processes/${id}/activate`).then(() => undefined),

  terminate: (id: string, reason?: string) =>
    api.delete<void>(`/processes/${id}`, { params: { reason: reason ?? 'Terminated via UI' } }).then(() => undefined),
}

// ── Activities (history — all node types) ─────────────────────────────────────

export const activitiesApi = {
  byInstance: (instanceId: string) =>
    api.get<ActivityInstance[]>(`/processes/${instanceId}/activities`).then(r => r.data),
}

// ── Tasks ─────────────────────────────────────────────────────────────────────

export const tasksApi = {
  listOpen: () =>
    api.get<TaskEntity[]>('/tasks').then(r => r.data),

  get: (id: string) =>
    api.get<TaskEntity>(`/tasks/${id}`).then(r => r.data),

  complete: (id: string, variables?: Record<string, unknown>) =>
    api.post<void>(`/tasks/${id}/complete`, variables ?? {}).then(() => undefined),

  claim: (id: string, userId: string) =>
    api.post<void>(`/tasks/${id}/claim`, null, { params: { userId } }).then(() => undefined),

  byAssignee: (userId: string) =>
    api.get<TaskEntity[]>(`/tasks/assignee/${userId}`).then(r => r.data),

  byInstance: (instanceId: string) =>
    api.get<TaskEntity[]>(`/processes/${instanceId}/tasks`).then(r => r.data),

  detail: (taskId: string) =>
    api.get<TaskDetailDto>(`/tasks/${taskId}/detail`).then(r => r.data),
}

// ── Messages & Signals ────────────────────────────────────────────────────────

export const eventsApi = {
  correlateMessage: (name: string, businessKey?: string, variables?: Record<string, unknown>) =>
    api.post<void>(`/messages/${name}/correlate`, { businessKey, variables }).then(() => undefined),

  broadcastSignal: (name: string, variables?: Record<string, unknown>) =>
    api.post<void>(`/signals/${name}/broadcast`, variables ?? {}).then(() => undefined),
}

// ── Users ─────────────────────────────────────────────────────────────────────

export const usersApi = {
  list: (page = 0, size = 20) =>
    api.get<Page<UserDto>>('/identity/users', { params: { page, size } }).then(r => r.data),

  getById: (id: string) =>
    api.get<UserDto>(`/identity/users/${id}`).then(r => r.data),

  getByUsername: (username: string) =>
    api.get<UserDto>(`/identity/users/by-username/${username}`).then(r => r.data),

  create: (req: CreateUserRequest) =>
    api.post<UserDto>('/identity/users', req).then(r => r.data),

  update: (id: string, req: UpdateUserRequest) =>
    api.put<UserDto>(`/identity/users/${id}`, req).then(r => r.data),

  delete: (id: string) =>
    api.delete<void>(`/identity/users/${id}`).then(() => undefined),

  assignRole: (id: string, roleName: string) =>
    api.post<UserDto>(`/identity/users/${id}/roles/${roleName}`).then(r => r.data),

  revokeRole: (id: string, roleName: string) =>
    api.delete<UserDto>(`/identity/users/${id}/roles/${roleName}`).then(r => r.data),

  getEffectiveRoles: (id: string) =>
    api.get<string[]>(`/identity/users/${id}/roles`).then(r => r.data),

  getMemberships: (id: string) =>
    api.get<MembershipDto[]>(`/identity/users/${id}/memberships`).then(r => r.data),

  joinGroup: (id: string, groupName: string, addedBy?: string) =>
    api.post<MembershipDto>(`/identity/users/${id}/memberships`, { groupName, addedBy }).then(r => r.data),

  leaveGroup: (id: string, groupName: string) =>
    api.delete<void>(`/identity/users/${id}/memberships/${groupName}`).then(() => undefined),

  byGroup: (groupName: string) =>
    api.get<UserDto[]>(`/identity/users/by-group/${groupName}`).then(r => r.data),

  byRole: (roleName: string) =>
    api.get<UserDto[]>(`/identity/users/by-role/${roleName}`).then(r => r.data),

  verifyPassword: (username: string, password: string) =>
    api.post<{ valid: boolean }>('/identity/users/verify-password', { username, password }).then(r => r.data.valid),
}

// ── Roles ─────────────────────────────────────────────────────────────────────

export const rolesApi = {
  list: () =>
    api.get<RoleDto[]>('/identity/roles').then(r => r.data),

  getById: (id: string) =>
    api.get<RoleDto>(`/identity/roles/${id}`).then(r => r.data),

  create: (req: CreateRoleRequest) =>
    api.post<RoleDto>('/identity/roles', req).then(r => r.data),

  update: (id: string, req: CreateRoleRequest) =>
    api.put<RoleDto>(`/identity/roles/${id}`, req).then(r => r.data),

  delete: (id: string) =>
    api.delete<void>(`/identity/roles/${id}`).then(() => undefined),
}

// ── Groups ────────────────────────────────────────────────────────────────────

export const groupsApi = {
  list: (type?: string) =>
    api.get<GroupDto[]>('/identity/groups', { params: type ? { type } : {} }).then(r => r.data),

  getById: (id: string) =>
    api.get<GroupDto>(`/identity/groups/${id}`).then(r => r.data),

  create: (req: CreateGroupRequest) =>
    api.post<GroupDto>('/identity/groups', req).then(r => r.data),

  update: (id: string, req: CreateGroupRequest) =>
    api.put<GroupDto>(`/identity/groups/${id}`, req).then(r => r.data),

  delete: (id: string) =>
    api.delete<void>(`/identity/groups/${id}`).then(() => undefined),

  assignRole: (id: string, roleName: string) =>
    api.post<GroupDto>(`/identity/groups/${id}/roles/${roleName}`).then(r => r.data),

  removeRole: (id: string, roleName: string) =>
    api.delete<GroupDto>(`/identity/groups/${id}/roles/${roleName}`).then(r => r.data),

  getMembers: (id: string) =>
    api.get<MembershipDto[]>(`/identity/groups/${id}/members`).then(r => r.data),
}
