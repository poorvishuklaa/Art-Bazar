import { useEffect, useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createArtwork, fetchMyArtwork, updateArtwork } from '../../api/artworks'
import { getErrorMessage, getFieldErrors } from '../../api/client'
import { useCategories } from '../../hooks/useCategories'
import { useAuth } from '../../hooks/useAuth'
import FormField from '../../components/ui/FormField'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import { usePageTitle } from '../../hooks/usePageTitle'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

// Mirrors the backend ArtworkRequest rules.
const schema = z.object({
  title: z.string().trim().min(3, 'Title must be at least 3 characters').max(150, 'Title can be at most 150 characters'),
  description: z.string().max(5000, 'Description can be at most 5000 characters').optional(),
  price: z.coerce
    .number({ error: 'Enter a price' })
    .min(1, 'Price must be at least ₹1')
    .max(10000000, 'Price is too high')
    .refine((v) => Number.isInteger(Math.round(v * 100)) && Math.abs(v * 100 - Math.round(v * 100)) < 1e-6, 'At most 2 decimal places'),
  stock: z.coerce.number({ error: 'Enter how many you have' }).int('Whole numbers only').min(0, 'Cannot be negative').max(10000),
  categoryId: z.coerce.number({ error: 'Choose a category' }).int().positive('Choose a category'),
  status: z.enum(['ACTIVE', 'HIDDEN']),
})

const ArtworkEditor = () => {
  const { id } = useParams()
  const isEdit = Boolean(id)
  usePageTitle(isEdit ? 'Edit artwork' : 'Add artwork')
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { user } = useAuth()
  const categories = useCategories().filter((c) => c.id)

  const [image, setImage] = useState(null)
  const [imageError, setImageError] = useState('')
  const [serverError, setServerError] = useState('')

  const existing = useQuery({ queryKey: ['my-artwork', id], queryFn: () => fetchMyArtwork(id), enabled: isEdit })

  const {
    register,
    handleSubmit,
    reset,
    setError,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { title: '', description: '', price: '', stock: 1, categoryId: '', status: 'ACTIVE' },
  })

  useEffect(() => {
    if (existing.data) {
      const a = existing.data
      reset({
        title: a.title,
        description: a.description ?? '',
        price: a.price,
        stock: a.stock,
        categoryId: a.category.id,
        status: a.status,
      })
    }
  }, [existing.data, reset])

  const previewUrl = useMemo(() => (image ? URL.createObjectURL(image) : null), [image])
  useEffect(() => () => previewUrl && URL.revokeObjectURL(previewUrl), [previewUrl])

  const save = useMutation({
    mutationFn: (values) => (isEdit ? updateArtwork(id, values, image) : createArtwork(values, image)),
    onSuccess: (saved) => {
      queryClient.invalidateQueries({ queryKey: ['my-artworks'] })
      queryClient.invalidateQueries({ queryKey: ['artworks'] })
      queryClient.invalidateQueries({ queryKey: ['artwork', String(saved.id)] })
      queryClient.removeQueries({ queryKey: ['my-artwork', id] })
      navigate('/artist', { state: { flash: `“${saved.title}” was ${isEdit ? 'updated' : 'added'}.` } })
    },
    onError: (err) => {
      const fields = getFieldErrors(err)
      Object.entries(fields).forEach(([field, message]) => setError(field, { message }))
      if (Object.keys(fields).length === 0) setServerError(getErrorMessage(err))
    },
  })

  if (!user.artistApproved) return <Navigate to="/artist" replace />

  const pickImage = (file) => {
    setImageError('')
    if (!file) return
    if (!IMAGE_TYPES.includes(file.type)) return setImageError('Choose a JPEG, PNG or WebP image')
    if (file.size > MAX_IMAGE_BYTES) return setImageError('Image must be 5 MB or smaller')
    setImage(file)
  }

  const missingImage = () => {
    if (!isEdit && !image) {
      setImageError('Add a photo of your artwork')
      return true
    }
    return false
  }

  const onSubmit = (values) => {
    setServerError('')
    if (missingImage()) return
    save.mutate(values)
  }

  if (isEdit && existing.isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (isEdit && existing.isError) {
    return (
      <div className="py-12">
        <Alert type="error">{getErrorMessage(existing.error, 'Artwork not found')}</Alert>
      </div>
    )
  }

  const shownImage = previewUrl ?? existing.data?.imageUrl

  return (
    <div className="mx-auto max-w-4xl py-10">
      <Link to="/artist" className="text-sm text-gray-500 hover:text-brand-500">
        ← My artworks
      </Link>
      <h1 className="mt-3 text-3xl font-medium">{isEdit ? 'Edit artwork' : 'Add a new artwork'}</h1>

      <form onSubmit={handleSubmit(onSubmit, missingImage)} noValidate className="mt-8 grid gap-8 md:grid-cols-[280px_1fr]">
        <div>
          <span className="mb-1.5 block text-sm font-medium text-gray-700">Photo</span>
          <label
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault()
              pickImage(e.dataTransfer.files?.[0])
            }}
            className={`flex aspect-square cursor-pointer flex-col items-center justify-center overflow-hidden rounded-2xl border-2 border-dashed text-center transition hover:border-brand-500 ${
              imageError ? 'border-red-400' : 'border-gray-300'
            }`}
          >
            {shownImage ? (
              <img src={shownImage} alt="Artwork preview" className="h-full w-full object-cover" />
            ) : (
              <span className="px-6 text-sm text-gray-500">
                <span className="font-medium text-brand-500">Choose a photo</span> or drag it here
                <br />
                JPEG, PNG or WebP, up to 5 MB
              </span>
            )}
            <input
              type="file"
              accept={IMAGE_TYPES.join(',')}
              className="sr-only"
              aria-label="Artwork photo"
              onChange={(e) => pickImage(e.target.files?.[0])}
            />
          </label>
          {shownImage && <p className="mt-2 text-xs text-gray-500">Click the photo to replace it.</p>}
          {imageError && <p className="mt-2 text-sm text-red-600">{imageError}</p>}
        </div>

        <div className="space-y-5">
          {serverError && <Alert type="error">{serverError}</Alert>}
          <FormField label="Title" error={errors.title?.message} {...register('title')} />
          <FormField
            label="Description"
            as="textarea"
            rows={5}
            hint="Materials, size, technique: anything a buyer would want to know."
            error={errors.description?.message}
            {...register('description')}
          />
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField label="Price (₹)" type="number" min="1" step="0.01" inputMode="decimal" error={errors.price?.message} {...register('price')} />
            <FormField label="Quantity available" type="number" min="0" step="1" error={errors.stock?.message} {...register('stock')} />
          </div>
          <FormField label="Category" as="select" error={errors.categoryId?.message} {...register('categoryId')}>
            <option value="">Choose a category</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </FormField>

          <fieldset>
            <legend className="mb-2 text-sm font-medium text-gray-700">Visibility</legend>
            <div className="flex flex-wrap gap-4 text-sm">
              <label className="flex items-center gap-2">
                <input type="radio" value="ACTIVE" className="accent-brand-500" {...register('status')} /> Listed in the shop
              </label>
              <label className="flex items-center gap-2">
                <input type="radio" value="HIDDEN" className="accent-brand-500" {...register('status')} /> Hidden (draft)
              </label>
            </div>
          </fieldset>

          <div className="flex gap-3 pt-2">
            <button
              type="submit"
              disabled={save.isPending}
              className="flex items-center gap-2 rounded-full bg-brand-500 px-8 py-3 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
            >
              {save.isPending && <Spinner className="h-4 w-4 border-white border-t-transparent" />}
              {isEdit ? 'Save changes' : 'Add artwork'}
            </button>
            <Link to="/artist" className="rounded-full px-6 py-3 ring-1 ring-gray-200 hover:bg-gray-50">
              Cancel
            </Link>
          </div>
        </div>
      </form>
    </div>
  )
}

export default ArtworkEditor
