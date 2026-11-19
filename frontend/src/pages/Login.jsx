import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import AuthCard from '../components/auth/AuthCard'
import FormField from '../components/ui/FormField'
import Alert from '../components/ui/Alert'
import Spinner from '../components/ui/Spinner'
import { useAuth } from '../hooks/useAuth'
import { getErrorMessage } from '../api/client'
import { destinationAfterLogin } from '../utils/roles'

const schema = z.object({
  email: z.email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
})

const Login = () => {
  const { login, isAuthenticated, user } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [serverError, setServerError] = useState('')
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm({ resolver: zodResolver(schema) })

  const from = location.state?.from?.pathname

  if (isAuthenticated) return <Navigate to={destinationAfterLogin(user, from)} replace />

  const onSubmit = async (values) => {
    setServerError('')
    try {
      const signedIn = await login(values)
      navigate(destinationAfterLogin(signedIn, from), { replace: true })
    } catch (err) {
      setServerError(getErrorMessage(err))
    }
  }

  return (
    <AuthCard
      title="Welcome back"
      subtitle="Sign in to buy art or manage your shop"
      footer={
        <>
          New to Art Bazaar?{' '}
          <Link to="/register" state={location.state} className="font-medium text-brand-500 hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-5">
        {serverError && <Alert type="error">{serverError}</Alert>}
        <FormField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <button
          type="submit"
          disabled={isSubmitting}
          className="flex w-full items-center justify-center gap-2 rounded-full bg-brand-500 py-3 font-medium text-white transition hover:bg-brand-600 disabled:opacity-60"
        >
          {isSubmitting && <Spinner className="h-4 w-4 border-white border-t-transparent" />}
          Sign in
        </button>
      </form>
    </AuthCard>
  )
}

export default Login
