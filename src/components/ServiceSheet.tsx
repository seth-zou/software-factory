import { useTranslation } from 'react-i18next'
import { BellRing, CheckCircle2, Clock3, Droplets, GlassWater, Receipt, Soup, Utensils } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import type { ServiceRequest } from '@/types'

const services = [
  { key: 'service.broth', icon: Soup },
  { key: 'service.drinks', icon: GlassWater },
  { key: 'service.utensils', icon: Utensils },
  { key: 'service.bill', icon: Receipt },
]

interface ServiceSheetProps {
  open: boolean
  requests: ServiceRequest[]
  onOpenChange: (value: boolean) => void
  onCall: (service: string) => void
}

export function ServiceSheet({ open, requests, onOpenChange, onCall }: ServiceSheetProps) {
  const { t } = useTranslation()
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={t('service.title')}>
        <p className="mt-2 text-sm text-charcoal-500">{t('service.desc')}</p>
        <div className="mt-5 grid grid-cols-2 gap-3">
          {services.map(({ key, icon: Icon }) => (
            <button key={key} onClick={() => onCall(key)} className="rounded-2xl border border-charcoal-900/5 bg-white p-4 text-left shadow-sm transition hover:-translate-y-1 hover:border-chili-500/30 hover:shadow-card">
              <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-chili-50 text-chili-500"><Icon size={20} /></span>
              <p className="mt-3 font-bold text-charcoal-900">{t(`${key}.name`)}</p><p className="mt-1 text-xs leading-5 text-charcoal-500">{t(`${key}.desc`)}</p>
            </button>
          ))}
        </div>
        <div className="mt-5 rounded-2xl bg-rice-100 p-4">
          <div className="flex items-center gap-2 font-bold text-charcoal-900"><BellRing size={17} className="text-amber-500" />{t('service.record')}</div>
          {!requests.length && <p className="mt-3 text-sm text-charcoal-500">{t('service.empty')}</p>}
          <div className="mt-3 space-y-2">
            {requests.slice().reverse().map((request) => (
              <div key={request.id} className="flex items-center justify-between rounded-xl bg-white p-3 text-sm">
                <span className="font-semibold text-charcoal-900">{request.type} <small className="ml-1 font-normal text-charcoal-500">{request.createdAt}</small></span>
                <span className={`flex items-center gap-1 text-xs font-bold ${request.status === 'responded' ? 'text-emerald-600' : 'text-amber-500'}`}>{request.status === 'responded' ? <CheckCircle2 size={14} /> : <Clock3 size={14} />}{request.status === 'responded' ? t('service.responded') : t('service.waiting')}</span>
              </div>
            ))}
          </div>
        </div>
        <Button variant="outline" className="mt-4 w-full" onClick={() => onCall('service.broth')}><Droplets size={17} />{t('service.one_click_broth')}</Button>
      </DialogContent>
    </Dialog>
  )
}
