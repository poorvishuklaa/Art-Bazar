import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { changePassword, fetchAccount, updateAccount } from '../api/account'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { useAuth } from '../hooks/useAuth'
import { formatDate } from '../utils/format'
import FormField from '../components/ui/FormField'
import Alert from '../components/ui/Alert'
import Spinner from '../components/ui/Spinner'
import { usePageTitle } from '../hooks/usePageTitle'

const profileSchema = z.object({
  name: z.string().trim().min(2, 'Name must be at least 2 characters').max(100),
  city: z.string().max(100).optional(),
  bio: z.string().max(1000, 'Bio can be at most 1000 characters').optional(),
})

const passwordSchema = z
  .object({
    currentPassword: z.string().min(1, 'Enter your current password'),
    newPassword: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72)
      .regex(/^(?=.*[A-Za-z])(?=.*\d)/, 'Password must contain a letter and a number'),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { path: ['confirm'], message: 'Passwords do not match' })

const Card = ({ title, subtitle, children }) => (
  <section className="rounded-2xl bg-white p-6 ring-1 ring-gray-100">
    <h2 className="text-lg font-medium">{title}</h2>
    {subtitle && <p className="mt-1 text-sm text-gray-500">{subtitle}</p>}
    <div className="mt-5">{children}</div>
  </section>
)

const ProfileForm = ({ account }) => {
  const { reloadUser } = useAuth()
  const queryClient = useQueryClient()
  const isArtist = account.role === 'ARTIST'
  const { register, handleSubmit, reset, setError, formState: { errors, isDirty } } = useForm({
    resolver: zodResolver(profileSchema),
    defaultValues: { name: account.name, city: account.city ?? '', bio: account.bio ?? '' },
  })
  const save = useMutation({
    mutationFn: updateAccount,
    onSuccess: (updated) => {
      queryClient.setQueryData(['account'], updated)
      reset({ name: updated.name, city: updated.city ?? '', bio: updated.bio ?? '' })
      reloadUser().catch(() => {})
      queryClient.invalidateQueries({ queryKey: ['artist'] })
    },
    onError: (err) => Object.entries(getFieldErrors(err)).forEach(([f, message]) => setError(f, { message })),
  })

  return (
    <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-5">
      {save.isSuccess && !isDirty && <Alert type="success">Profile saved.</Alert>}
      {save.isError && Object.keys(getFieldErrors(save.error)).length === 0 && <Alert type="error">{getErrorMessage(save.error)}</Alert>}
      <FormField label="Full name" autoComplete="name" error={errors.name?.message} {...register('name')} />
      <div>
        <span className="mb-1.5 block text-sm font-medium text-gray-700">Email</span>
        <p className="rounded-lg bg-gray-50 px-3.5 py-2.5 text-gray-600">{account.email}</p>
      </div>
      {isArtist && (
        <>
          <FormField label="City" autoComplete="address-level2" error={errors.city?.message} {...register('city')} />
          <FormField label="About your art" as="textarea" rows={4} hint="Shown on your public artist page." error={errors.bio?.message} {...register('bio')} />
        </>
      )}
      <button type="submit" disabled={save.isPending || !isDirty} className="rounded-full bg-brand-500 px-6 py-2.5 font-medium text-white hover:bg-brand-600 disabled:opacity-50">
        Save profile
      </button>
    </form>
  )
}

const PasswordForm = () => {
  const { replaceTokens } = useAuth()
  const [done, setDone] = useState(false)
  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm({ resolver: zodResolver(passwordSchema) })
  const save = useMutation({
    mutationFn: ({ currentPassword, newPassword }) => changePassword(currentPassword, newPassword),
    onSuccess: (session) => {
      replaceTokens(session)
      reset({ currentPassword: '', newPassword: '', confirm: '' })
      setDone(true)
    },
    onError: (err) => {
      const fields = getFieldErrors(err)
      Object.entries(fields).forEach(([f, message]) => setError(f, { message }))
      if (Object.keys(fields).length === 0) setError('currentPassword', { message: getErrorMessage(err) })
    },
  })

  return (
    <form noValidate onSubmit={handleSubmit((v) => { setDone(false); save.mutate(v) })} className="space-y-5">
      {done && <Alert type="success">Password changed. You&apos;ve been signed out on all your other devices.</Alert>}
      <FormField label="Current password" type="password" autoComplete="current-password" error={errors.currentPassword?.message} {...register('currentPassword')} />
      <FormField label="New password" type="password" autoComplete="new-password" hint="At least 8 characters, with a letter and a number" error={errors.newPassword?.message} {...register('newPassword')} />
      <FormField label="Confirm new password" type="password" autoComplete="new-password" error={errors.confirm?.message} {...register('confirm')} />
      <button type="submit" disabled={save.isPending} className="rounded-full bg-ink px-6 py-2.5 font-medium text-white hover:bg-black disabled:opacity-50">
        Change password
      </button>
    </form>
  )
}

const Account = () => {
  const { data: account, isPending, isError, error } = useQuery({ queryKey: ['account'], queryFn: fetchAccount })
  usePageTitle('Account settings')

  if (isPending) return <div className="grid place-items-center py-32"><Spinner className="h-8 w-8" /></div>
  if (isError) return <div className="py-12"><Alert type="error">{getErrorMessage(error)}</Alert></div>

  return (
    <div className="mx-auto max-w-2xl space-y-6 py-10">
      <div>
        <h1 className="text-3xl font-medium">Account settings</h1>
        <p className="mt-1 text-gray-500">
          {account.role.charAt(0) + account.role.slice(1).toLowerCase()} account · member since {formatDate(account.memberSince)}
        </p>
      </div>
      <Card title="Profile" subtitle={account.role === 'ARTIST' ? 'Your name, city and bio appear on your public artist page.' : undefined}>
        <ProfileForm account={account} />
      </Card>
      <Card title="Password" subtitle="Changing it signs you out everywhere except here.">
        <PasswordForm />
      </Card>
    </div>
  )
}

export default Account
