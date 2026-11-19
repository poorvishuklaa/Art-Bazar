import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchAdminUsers, setUserEnabled } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { formatDate } from '../../utils/format'
import AdminNav from '../../components/admin/AdminNav'
import SearchBox from '../../components/admin/SearchBox'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import EmptyState from '../../components/ui/EmptyState'
import Pagination from '../../components/ui/Pagination'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'

const roles = [['', 'All roles'], ['BUYER', 'Buyers'], ['ARTIST', 'Artists'], ['ADMIN', 'Admins']]

const AdminUsers = () => {
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  const role = params.get('role') ?? ''
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)
  const [target, setTarget] = useState(null)

  const update = (changes) => setParams((prev) => {
    const next = new URLSearchParams(prev)
    Object.entries(changes).forEach(([k, v]) => (v ? next.set(k, v) : next.delete(k)))
    if (!('page' in changes)) next.delete('page')
    return next
  }, { replace: true })

  const users = useQuery({
    queryKey: ['admin', 'users', q, role, page],
    queryFn: () => fetchAdminUsers({ q, role, page }),
    placeholderData: keepPreviousData,
  })

  const toggle = useMutation({
    mutationFn: (u) => setUserEnabled(u.id, !u.enabled),
    onSuccess: () => {
      setTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin'] })
    },
    onError: () => setTarget(null),
  })

  return (
    <div className="py-10">
      <AdminNav />
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-medium">Users</h1>
          <p className="mt-1 text-gray-500">Disabling an account signs that person out everywhere, straight away.</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <SearchBox value={q} onChange={(v) => update({ q: v })} placeholder="Name or email" label="Search users" />
          <select
            aria-label="Role"
            value={role}
            onChange={(e) => update({ role: e.target.value })}
            className="rounded-full border border-gray-300 bg-white px-4 py-2 text-sm outline-none focus:border-brand-500"
          >
            {roles.map(([v, l]) => <option key={v} value={v}>{l}</option>)}
          </select>
        </div>
      </div>

      <div className="mt-6 space-y-4">
        {toggle.isError && <Alert type="error">{getErrorMessage(toggle.error)}</Alert>}
        {users.isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : users.isError ? (
          <Alert type="error">{getErrorMessage(users.error)}</Alert>
        ) : users.data.content.length === 0 ? (
          <EmptyState title="No users match" />
        ) : (
          <>
            <ul className="divide-y divide-gray-100 rounded-2xl bg-white ring-1 ring-gray-100">
              {users.data.content.map((u) => (
                <li key={u.id} className="flex flex-col gap-3 p-4 sm:flex-row sm:items-center">
                  <span className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-brand-50 font-semibold text-brand-500">
                    {u.name.charAt(0).toUpperCase()}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-2">
                      <span className="font-medium">{u.name}</span>
                      <span className="rounded-full bg-gray-100 px-2 py-0.5 text-[11px] font-semibold tracking-wide text-gray-600 uppercase">{u.role.toLowerCase()}</span>
                      {u.artistApproved === false && <span className="text-xs text-amber-700">Not approved</span>}
                      {!u.enabled && <span className="rounded-full bg-red-50 px-2 py-0.5 text-xs font-medium text-red-700 ring-1 ring-red-200">Disabled</span>}
                    </div>
                    <p className="text-sm break-all text-gray-500">{u.email} · joined {formatDate(u.joinedAt)}</p>
                  </div>
                  {u.role !== 'ADMIN' && (
                    <button
                      type="button"
                      onClick={() => setTarget(u)}
                      className={`shrink-0 rounded-full px-4 py-1.5 text-sm ring-1 ${u.enabled ? 'text-red-600 ring-red-200 hover:bg-red-50' : 'text-green-700 ring-green-200 hover:bg-green-50'}`}
                    >
                      {u.enabled ? 'Disable' : 'Enable'}
                    </button>
                  )}
                </li>
              ))}
            </ul>
            <Pagination page={users.data.page} totalPages={users.data.totalPages} onChange={(p) => update({ page: p ? String(p + 1) : '' })} />
          </>
        )}
      </div>

      <ConfirmDialog
        open={!!target}
        title={target?.enabled ? `Disable ${target?.name}?` : `Enable ${target?.name}?`}
        confirmLabel={target?.enabled ? 'Disable account' : 'Enable account'}
        danger={target?.enabled}
        busy={toggle.isPending}
        onCancel={() => setTarget(null)}
        onConfirm={() => toggle.mutate(target)}
      >
        {target?.enabled
          ? 'They will be signed out on every device and won’t be able to sign in. Their past orders and artworks are kept.'
          : 'They will be able to sign in again.'}
      </ConfirmDialog>
    </div>
  )
}

export default AdminUsers
