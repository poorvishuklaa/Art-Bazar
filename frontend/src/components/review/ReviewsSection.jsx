import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminDeleteReview, deleteMyReview, fetchMyReview, fetchReviews, saveMyReview } from '../../api/reviews'
import { getErrorMessage } from '../../api/client'
import { useAuth } from '../../hooks/useAuth'
import { formatDate } from '../../utils/format'
import Stars from './Stars'
import StarInput from './StarInput'
import Alert from '../ui/Alert'
import ConfirmDialog from '../ui/ConfirmDialog'

/** A review changes the artwork's rating everywhere it is shown. */
const refreshRatings = (queryClient) => {
  const keys = ['reviews', 'my-review', 'artwork', 'artworks']
  keys.forEach((key) => queryClient.invalidateQueries({ queryKey: [key] }))
}

/** Count per star as thin horizontal bars (one hue, label + number beside each, so not colour-only). */
const Distribution = ({ distribution, count }) => (
  <ul className="space-y-1.5" aria-label="Rating breakdown">
    {[5, 4, 3, 2, 1].map((star) => {
      const n = distribution[star] ?? 0
      const pct = count ? (n / count) * 100 : 0
      return (
        <li key={star} className="flex items-center gap-2 text-xs text-gray-600">
          <span className="w-10 shrink-0">{star} star</span>
          <span className="h-2 flex-1 overflow-hidden rounded-full bg-gray-100">
            <span className="block h-full rounded-full bg-amber-400" style={{ width: `${pct}%` }} />
          </span>
          <span className="w-6 shrink-0 text-right tabular-nums">{n}</span>
        </li>
      )
    })}
  </ul>
)

const ReviewForm = ({ artworkId, existing, onDone }) => {
  const queryClient = useQueryClient()
  const [rating, setRating] = useState(existing?.rating ?? 0)
  const [comment, setComment] = useState(existing?.comment ?? '')
  const [missing, setMissing] = useState(false)
  const save = useMutation({
    mutationFn: () => saveMyReview(artworkId, { rating, comment }),
    onSuccess: () => {
      refreshRatings(queryClient)
      onDone()
    },
  })
  return (
    <form
      noValidate
      onSubmit={(e) => {
        e.preventDefault()
        if (!rating) return setMissing(true)
        save.mutate()
      }}
      className="space-y-4 rounded-2xl bg-gray-50 p-5"
    >
      <StarInput value={rating} onChange={(n) => { setRating(n); setMissing(false) }} />
      {missing && <p className="text-sm text-red-600">Choose a star rating</p>}
      <label className="block text-sm font-medium text-gray-700">
        Your review <span className="font-normal text-gray-500">(optional)</span>
        <textarea
          value={comment}
          onChange={(e) => setComment(e.target.value)}
          maxLength={1000}
          rows={3}
          placeholder="How does it look in your home? Was it as described?"
          className="mt-1.5 block w-full rounded-lg border border-gray-300 px-3.5 py-2.5 font-normal outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
        />
      </label>
      {save.isError && <Alert type="error">{getErrorMessage(save.error)}</Alert>}
      <div className="flex gap-2">
        <button type="submit" disabled={save.isPending} className="rounded-full bg-brand-500 px-6 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60">
          {existing ? 'Update review' : 'Post review'}
        </button>
        {existing && (
          <button type="button" onClick={onDone} className="rounded-full px-5 py-2 text-sm ring-1 ring-gray-200 hover:bg-white">Cancel</button>
        )}
      </div>
    </form>
  )
}

/** Reviews of one artwork, plus "write a review" for buyers who purchased it. */
const ReviewsSection = ({ artworkId }) => {
  const { user, isAuthenticated } = useAuth()
  const location = useLocation()
  const queryClient = useQueryClient()
  const [page, setPage] = useState(0)
  const [editing, setEditing] = useState(false)
  const [confirm, setConfirm] = useState(null) // { kind: 'mine' } | { kind: 'admin', review }

  const reviews = useQuery({
    queryKey: ['reviews', artworkId, page],
    queryFn: () => fetchReviews(artworkId, page),
    placeholderData: keepPreviousData,
  })
  const mine = useQuery({
    queryKey: ['my-review', artworkId],
    queryFn: () => fetchMyReview(artworkId),
    enabled: isAuthenticated,
  })

  useEffect(() => {
    if (location.hash === '#reviews') document.getElementById('reviews')?.scrollIntoView({ behavior: 'smooth' })
  }, [location.hash, reviews.isSuccess])

  const remove = useMutation({
    mutationFn: (c) => (c.kind === 'admin' ? adminDeleteReview(c.review.id) : deleteMyReview(artworkId)),
    onSuccess: () => {
      setConfirm(null)
      refreshRatings(queryClient)
    },
    onError: () => setConfirm(null),
  })

  const data = reviews.data
  const myReview = mine.data?.review
  const showForm = mine.data?.canReview && (!myReview || editing)

  return (
    <section id="reviews" className="mt-16 scroll-mt-24">
      <h2 className="text-2xl font-medium">Reviews</h2>
      {reviews.isError && <div className="mt-4"><Alert type="error">{getErrorMessage(reviews.error)}</Alert></div>}
      {data && (
        <div className="mt-6 grid gap-10 lg:grid-cols-[280px_1fr]">
          <div>
            {data.count > 0 ? (
              <>
                <p className="text-4xl font-semibold">{data.average.toFixed(1)}</p>
                <Stars rating={data.average} size="h-5 w-5" className="mt-1" />
                <p className="mt-1 text-sm text-gray-500">{data.count} {data.count === 1 ? 'review' : 'reviews'} from verified buyers</p>
                <div className="mt-5"><Distribution distribution={data.distribution} count={data.count} /></div>
              </>
            ) : (
              <p className="text-sm text-gray-500">No reviews yet. Reviews come only from people who bought this piece.</p>
            )}
          </div>

          <div className="space-y-6">
            {remove.isError && <Alert type="error">{getErrorMessage(remove.error)}</Alert>}
            {showForm && <ReviewForm key={myReview?.id ?? 'new'} artworkId={artworkId} existing={myReview} onDone={() => setEditing(false)} />}
            {myReview && !editing && (
              <div className="rounded-2xl bg-brand-50 p-5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-sm font-medium">Your review</p>
                  <div className="flex gap-3 text-sm">
                    <button type="button" onClick={() => setEditing(true)} className="text-brand-500 hover:underline">Edit</button>
                    <button type="button" onClick={() => setConfirm({ kind: 'mine' })} className="text-gray-500 hover:text-red-600">Delete</button>
                  </div>
                </div>
                <Stars rating={myReview.rating} className="mt-2" />
                {myReview.comment && <p className="mt-2 text-gray-700">{myReview.comment}</p>}
              </div>
            )}
            {!isAuthenticated && data.count > 0 && (
              <p className="text-sm text-gray-500">
                Bought this piece? <Link to="/login" state={{ from: { pathname: location.pathname, hash: '#reviews' } }} className="text-brand-500 hover:underline">Sign in</Link> to review it.
              </p>
            )}

            <ul className="divide-y divide-gray-100">
              {data.reviews.content.filter((r) => r.id !== myReview?.id).map((r) => (
                <li key={r.id} className="py-4 first:pt-0">
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-1">
                    <Stars rating={r.rating} />
                    <span className="text-sm font-medium">{r.reviewerName}</span>
                    <span className="text-xs text-gray-400">{formatDate(r.createdAt)}{r.edited && ' · edited'}</span>
                    {user?.role === 'ADMIN' && (
                      <button type="button" onClick={() => setConfirm({ kind: 'admin', review: r })} className="ml-auto text-xs text-red-600 hover:underline">
                        Remove
                      </button>
                    )}
                  </div>
                  {r.comment && <p className="mt-1.5 text-gray-700">{r.comment}</p>}
                </li>
              ))}
            </ul>
            {data.reviews.totalPages > 1 && (
              <div className="flex gap-3 text-sm">
                <button type="button" disabled={page === 0} onClick={() => setPage((p) => p - 1)} className="rounded-full px-4 py-1.5 ring-1 ring-gray-200 disabled:opacity-40">Newer</button>
                <button type="button" disabled={data.reviews.last} onClick={() => setPage((p) => p + 1)} className="rounded-full px-4 py-1.5 ring-1 ring-gray-200 disabled:opacity-40">Older</button>
              </div>
            )}
          </div>
        </div>
      )}

      <ConfirmDialog
        open={!!confirm}
        title={confirm?.kind === 'admin' ? 'Remove this review?' : 'Delete your review?'}
        confirmLabel={confirm?.kind === 'admin' ? 'Remove' : 'Delete'}
        danger
        busy={remove.isPending}
        onCancel={() => setConfirm(null)}
        onConfirm={() => remove.mutate(confirm)}
      >
        {confirm?.kind === 'admin' ? 'It will be removed for everyone and no longer count towards the rating.' : 'You can write a new one later.'}
      </ConfirmDialog>
    </section>
  )
}

export default ReviewsSection
