import { useCallback, useEffect, useState } from 'react'
import { fetchArtistApplications, setArtistApproval } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'

const tabs = [
  { key: 'pending', label: 'Pending', approved: false },
  { key: 'approved', label: 'Approved', approved: true },
]

const dateFormat = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' })

const ArtistApplications = () => {
  const [tab, setTab] = useState('pending')
  const [artists, setArtists] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setArtists(await fetchArtistApplications(tabs.find((t) => t.key === tab).approved))
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [tab])

  useEffect(() => {
    load()
  }, [load])

  const decide = async (artist, approved) => {
    setBusyId(artist.userId)
    setError('')
    try {
      await setArtistApproval(artist.userId, approved)
      setArtists((list) => list.filter((a) => a.userId !== artist.userId))
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="py-12">
      <h1 className="text-3xl font-medium">Artist applications</h1>
      <p className="mt-2 text-gray-500">Approve artists before they can list artworks on the marketplace.</p>

      <div role="tablist" className="mt-8 flex gap-2 border-b border-gray-200">
        {tabs.map((t) => (
          <button
            key={t.key}
            role="tab"
            aria-selected={tab === t.key}
            onClick={() => setTab(t.key)}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${
              tab === t.key ? 'border-brand-500 text-brand-500' : 'border-transparent text-gray-500 hover:text-ink'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      <div className="mt-6 space-y-4">
        {error && <Alert type="error">{error}</Alert>}
        {loading ? (
          <div className="grid place-items-center py-16">
            <Spinner className="h-8 w-8" />
          </div>
        ) : artists.length === 0 ? (
          <p className="rounded-xl bg-gray-50 p-8 text-center text-gray-500">
            {tab === 'pending' ? 'No applications waiting. You are all caught up!' : 'No approved artists yet.'}
          </p>
        ) : (
          artists.map((a) => (
            <article
              key={a.userId}
              className="flex flex-col gap-4 rounded-xl bg-white p-5 ring-1 ring-gray-100 sm:flex-row sm:items-start sm:justify-between"
            >
              <div className="min-w-0">
                <h2 className="font-medium">{a.name}</h2>
                <p className="text-sm break-all text-gray-500">
                  {a.email}
                  {a.city && ` · ${a.city}`}
                </p>
                {a.bio && <p className="mt-2 text-sm text-gray-700">{a.bio}</p>}
                <p className="mt-2 text-xs text-gray-400">
                  Applied {dateFormat.format(new Date(a.registeredAt))}
                  {a.approvedAt && ` · Approved ${dateFormat.format(new Date(a.approvedAt))}`}
                </p>
              </div>
              <div className="flex shrink-0 gap-2">
                {a.approved ? (
                  <button
                    onClick={() => decide(a, false)}
                    disabled={busyId === a.userId}
                    className="rounded-full border border-gray-300 px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-60"
                  >
                    Revoke approval
                  </button>
                ) : (
                  <button
                    onClick={() => decide(a, true)}
                    disabled={busyId === a.userId}
                    className="rounded-full bg-brand-500 px-5 py-2 text-sm font-medium text-white hover:bg-brand-600 disabled:opacity-60"
                  >
                    Approve
                  </button>
                )}
              </div>
            </article>
          ))
        )}
      </div>
    </div>
  )
}

export default ArtistApplications
