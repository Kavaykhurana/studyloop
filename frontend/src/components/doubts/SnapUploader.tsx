import { Camera } from 'lucide-react'
import { useEffect, useRef, useState, type DragEvent } from 'react'
import { useSnap } from '../../hooks/useDoubt'
import { errorMessage } from '../../lib/api'
import { cn } from '../../lib/cn'
import { compressImage } from '../../lib/image'
import type { SnapResult } from '../../lib/types'
import { Spinner } from '../ui/Spinner'

const ACCEPTED = ['image/jpeg', 'image/png', 'image/webp']

export function SnapUploader({ onResult }: { onResult: (result: SnapResult) => void }) {
  const input = useRef<HTMLInputElement>(null)
  const snap = useSnap()
  const [preview, setPreview] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [reading, setReading] = useState(false)
  const [dragging, setDragging] = useState(false)

  useEffect(() => () => {
    if (preview) URL.revokeObjectURL(preview)
  }, [preview])

  const handle = async (file: File | undefined) => {
    if (!file) return
    setError(null)
    if (!ACCEPTED.includes(file.type)) {
      setError('Upload a JPG, PNG or WebP image.')
      return
    }
    setPreview(URL.createObjectURL(file))
    setReading(true)
    try {
      const blob = await compressImage(file)
      const result = await snap.mutateAsync(blob)
      onResult(result)
    } catch (e) {
      setError(errorMessage(e, 'We could not read that photo. Try again.'))
    } finally {
      setReading(false)
    }
  }

  const onDrop = (event: DragEvent<HTMLButtonElement>) => {
    event.preventDefault()
    setDragging(false)
    void handle(event.dataTransfer.files[0])
  }

  return (
    <div className="flex flex-col gap-3">
      <button
        type="button"
        onClick={() => input.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        disabled={reading}
        className={cn(
          'flex h-[180px] w-full flex-col items-center justify-center gap-2 rounded-card border-2 border-dashed px-4 text-center transition-colors',
          dragging ? 'border-brand bg-brand-soft' : 'border-line-strong bg-surface hover:bg-surface-2',
        )}
      >
        {reading ? (
          <>
            <Spinner className="size-6 text-brand" />
            <span className="text-ink-2">Reading your photo…</span>
          </>
        ) : (
          <>
            <Camera className="size-8 text-ink-3" aria-hidden="true" />
            <span className="font-medium text-ink">Drop a photo of the question, or tap to take one</span>
            <span className="text-[12px] text-ink-3">JPG, PNG or WebP · up to 4 MB</span>
          </>
        )}
      </button>
      <input
        ref={input}
        type="file"
        accept="image/*"
        capture="environment"
        className="sr-only"
        aria-label="Photo of your question"
        onChange={(e) => {
          void handle(e.target.files?.[0])
          e.target.value = ''
        }}
      />
      {preview && (
        <img src={preview} alt="Your photo" className="max-h-48 w-auto self-start rounded-input border border-line" />
      )}
      {error && (
        <p role="alert" className="rounded-input bg-danger-soft px-3 py-2 text-[14px] text-danger">
          {error}
        </p>
      )}
    </div>
  )
}
