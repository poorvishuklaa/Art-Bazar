import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import AuthCard from '../components/auth/AuthCard'
import FormField from '../components/ui/FormField'
import Alert from '../components/ui/Alert'
import Spinner from '../components/ui/Spinner'
import { useAuth } from '../hooks/useAuth'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { homeFor } from '../utils/roles'

// Mirrors the backend RegisterRequest rules so most mistakes are caught before a round trip.
const schema = z
  .object({
    accountType: z.enum(['BUYER', 'ARTIST']),
    name: z.string().trim().min(2, 'Name must be at least 2 characters').max(100),
    email: z.email('Enter a valid email address'),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be at most 72 characters')
      .regex(/^(?=.*[A-Za-z])(?=.*\d)/, 'Password must contain a letter and a number'),
    confirmPassword: z.string(),
    city: z.string().max(100).optional(),
    bio: z.string().max(1000, 'Bio can be at most 1000 characters').optional(),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Passwords do not match',
  })

const accountTypes = [
  { value: 'BUYER', title: 'I want to buy', text: 'Discover and collect art' },
  { value: 'ARTIST', title: 'I want to sell', text: 'List my artworks' },
]

const Register = () => {
  const { register: signUp, isAuthenticated, user } = useAuth()
  const navigate = useNavigate()
  const [serverError, setServerError] = useState('')
  const {
    register,
    handleSubmit,
    watch,
    setError,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema), defaultValues: { accountType: 'BUYER' } })

  const accountType = watch('accountType')

  if (isAuthenticated) return <Navigate to={homeFor(user)} replace />

  const onSubmit = async (form) => {
    const values = { ...form }
    delete values.confirmPassword
    setServerError('')
    try {
      const created = await signUp(values)
      navigate(homeFor(created), { replace: true })
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      Object.entries(fieldErrors).forEach(([field, message]) => setError(field, { message }))
      if (Object.keys(fieldErrors).length === 0) setServerError(getErrorMessage(err))
    }
  }

  return (
    <AuthCard
      title="Create your account"
      subtitle="Join a community of local artists and art lovers"
      footer={
        <>
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-brand-500 hover:underline">
            Sign in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        {serverError && <Alert type="error">{serverError}</Alert>}

        <fieldset>
          <legend className="mb-2 text-sm font-medium text-gray-700">I am joining to</legend>
          <div className="grid grid-cols-2 gap-3">
            {accountTypes.map((t) => (
              <label
                key={t.value}
                className={`cursor-pointer rounded-xl border p-3 transition has-[:focus-visible]:ring-2 has-[:focus-visible]:ring-brand-100 ${
                  accountType === t.value ? 'border-brand-500 bg-brand-50' : 'border-gray-200 hover:border-gray-300'
                }`}
              >
                <input type="radio" value={t.value} className="sr-only" {...register('accountType')} />
                <span className="block font-medium">{t.title}</span>
                <span className="block text-xs text-gray-500">{t.text}</span>
              </label>
            ))}
          </div>
        </fieldset>

        <FormField label="Full name" autoComplete="name" error={errors.name?.message} {...register('name')} />
        <FormField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="At least 8 characters, with a letter and a number"
          error={errors.password?.message}
          {...register('password')}
        />
        <FormField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        {accountType === 'ARTIST' && (
          <>
            <FormField label="City" autoComplete="address-level2" error={errors.city?.message} {...register('city')} />
            <FormField
              label="About your art"
              as="textarea"
              rows={3}
              hint="Tell buyers what you make. You can change this later."
              error={errors.bio?.message}
              {...register('bio')}
            />
            <Alert type="info">New artist accounts are reviewed by our team before you can list artworks.</Alert>
          </>
        )}

        <button
          type="submit"
          disabled={isSubmitting}
          className="flex w-full items-center justify-center gap-2 rounded-full bg-brand-500 py-3 font-medium text-white transition hover:bg-brand-600 disabled:opacity-60"
        >
          {isSubmitting && <Spinner className="h-4 w-4 border-white border-t-transparent" />}
          Create account
        </button>
      </form>
    </AuthCard>
  )
}

export default Register
