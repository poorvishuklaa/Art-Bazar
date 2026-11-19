import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createCategory, deleteCategory, fetchAdminCategories, updateCategory } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import { fallbackCategories } from '../../data/categories'
import AdminNav from '../../components/admin/AdminNav'
import ConfirmDialog from '../../components/ui/ConfirmDialog'
import Spinner from '../../components/ui/Spinner'
import Alert from '../../components/ui/Alert'

const bundledIcon = Object.fromEntries(fallbackCategories.map((c) => [c.slug, c.iconUrl]))
const IMAGE_TYPES = 'image/jpeg,image/png,image/webp'

const Icon = ({ c }) => {
  const src = bundledIcon[c.slug] ?? c.iconUrl
  return src ? (
    <img src={src} alt="" className="h-12 w-12 rounded-full object-cover ring-1 ring-gray-200" />
  ) : (
    <span className="grid h-12 w-12 place-items-center rounded-full bg-brand-50 text-lg font-semibold text-brand-500">{c.name.charAt(0)}</span>
  )
}

/** Name + optional icon form, used for both "add" and "edit". */
const CategoryForm = ({ initialName = '', submitLabel, busy, onSubmit, onCancel }) => {
  const [name, setName] = useState(initialName)
  const [icon, setIcon] = useState(null)
  return (
    <form
      onSubmit={(e) => {
        e.preventDefault()
        onSubmit(name, icon)
      }}
      className="flex flex-col gap-3 sm:flex-row sm:items-end"
    >
      <label className="flex-1 text-sm font-medium text-gray-700">
        Name
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
          minLength={2}
          maxLength={60}
          className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 font-normal outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
        />
      </label>
      <label className="text-sm font-medium text-gray-700">
        Icon (optional)
        <input type="file" accept={IMAGE_TYPES} onChange={(e) => setIcon(e.target.files?.[0] ?? null)} className="mt-1 block w-full text-sm font-normal text-gray-600 file:mr-3 file:rounded-full file:border-0 file:bg-gray-100 file:px-3 file:py-1.5" />
      </label>
      <div className="flex gap-2">
        <button type="submit" disabled={busy} className="rounded-full bg-brand-500 px-5 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60">
          {submitLabel}
        </button>
        {onCancel && (
          <button type="button" onClick={onCancel} className="rounded-full px-4 py-2 text-sm ring-1 ring-gray-200 hover:bg-gray-50">Cancel</button>
        )}
      </div>
    </form>
  )
}

const AdminCategories = () => {
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState(null)
  const [deleting, setDeleting] = useState(null)
  const [formKey, setFormKey] = useState(0)

  const list = useQuery({ queryKey: ['admin', 'categories'], queryFn: fetchAdminCategories })
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['admin', 'categories'] })
    queryClient.invalidateQueries({ queryKey: ['categories'] })
  }

  const create = useMutation({
    mutationFn: ({ name, icon }) => createCategory(name, icon),
    onSuccess: () => { refresh(); setFormKey((k) => k + 1) },
  })
  const update = useMutation({
    mutationFn: ({ id, name, icon }) => updateCategory(id, name, icon),
    onSuccess: () => { refresh(); setEditing(null) },
  })
  const remove = useMutation({
    mutationFn: (c) => deleteCategory(c.id),
    onSuccess: () => { refresh(); setDeleting(null) },
    onError: () => setDeleting(null),
  })
  const error = create.error ?? update.error ?? remove.error

  return (
    <div className="py-10">
      <AdminNav />
      <h1 className="text-3xl font-medium">Categories</h1>
      <p className="mt-1 text-gray-500">Categories group artworks in the shop. Only empty categories can be deleted.</p>

      <section className="mt-6 rounded-2xl bg-gray-50 p-5">
        <h2 className="mb-3 font-medium">Add a category</h2>
        <CategoryForm key={formKey} submitLabel="Add" busy={create.isPending} onSubmit={(name, icon) => create.mutate({ name, icon })} />
      </section>

      <div className="mt-6 space-y-4">
        {error && <Alert type="error">{getErrorMessage(error)}</Alert>}
        {list.isPending ? (
          <div className="grid place-items-center py-16"><Spinner className="h-8 w-8" /></div>
        ) : list.isError ? (
          <Alert type="error">{getErrorMessage(list.error)}</Alert>
        ) : (
          <ul className="divide-y divide-gray-100 rounded-2xl bg-white ring-1 ring-gray-100">
            {list.data.map((c) => (
              <li key={c.id} className="p-4">
                {editing === c.id ? (
                  <CategoryForm
                    initialName={c.name}
                    submitLabel="Save"
                    busy={update.isPending}
                    onSubmit={(name, icon) => update.mutate({ id: c.id, name, icon })}
                    onCancel={() => setEditing(null)}
                  />
                ) : (
                  <div className="flex items-center gap-4">
                    <Icon c={c} />
                    <div className="min-w-0 flex-1">
                      <p className="font-medium">{c.name}</p>
                      <p className="text-sm text-gray-500">
                        /{c.slug} · {c.artworkCount} {c.artworkCount === 1 ? 'artwork' : 'artworks'}
                      </p>
                    </div>
                    <div className="flex shrink-0 gap-2 text-sm">
                      <button type="button" onClick={() => setEditing(c.id)} className="rounded-full px-3 py-1.5 ring-1 ring-gray-200 hover:bg-gray-50">Edit</button>
                      <button
                        type="button"
                        onClick={() => setDeleting(c)}
                        disabled={c.artworkCount > 0}
                        title={c.artworkCount > 0 ? 'Move or remove its artworks first' : undefined}
                        className="rounded-full px-3 py-1.5 text-red-600 ring-1 ring-red-200 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
                      >
                        Delete
                      </button>
                    </div>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </div>

      <ConfirmDialog
        open={!!deleting}
        title={`Delete “${deleting?.name}”?`}
        confirmLabel="Delete"
        danger
        busy={remove.isPending}
        onCancel={() => setDeleting(null)}
        onConfirm={() => remove.mutate(deleting)}
      >
        It has no artworks, so nothing else is affected.
      </ConfirmDialog>
    </div>
  )
}

export default AdminCategories
