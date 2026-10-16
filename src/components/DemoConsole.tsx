import { useTranslation } from 'react-i18next'
import { CheckCircle2, ChefHat, RotateCcw, Store, ToggleLeft, UtensilsCrossed, XCircle } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent } from '@/components/ui/dialog'
import { products, tableAreas } from '@/data/menu'
import type { OrderStage, ServiceRequest } from '@/types'

const stageIcons: Record<OrderStage, typeof ChefHat> = {
  submitted: Store,
  accepted: CheckCircle2,
  cooking: ChefHat,
  served: UtensilsCrossed,
}
const stageKeys: OrderStage[] = ['submitted', 'accepted', 'cooking', 'served']

interface DemoConsoleProps {
  open: boolean
  table: string
  stage: OrderStage
  soldOut: string[]
  services: ServiceRequest[]
  onOpenChange: (open: boolean) => void
  onStage: (stage: OrderStage) => void
  onSoldOut: (id: string) => void
  onRespond: () => void
  onReset: () => void
}

export function DemoConsole({ open, table, stage, soldOut, services, onOpenChange, onStage, onSoldOut, onRespond, onReset }: DemoConsoleProps) {
  const { t } = useTranslation()
  const waiting = services.filter((service) => service.status === 'waiting').length
  const areaKey = tableAreas[table]
  const tableLabel = areaKey ? `${table} · ${t(areaKey)}` : table
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent title={t('console.title')} className="md:max-w-2xl">
        <p className="mt-2 text-sm text-charcoal-500">{t('console.desc')}</p>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between"><h3 className="font-bold text-charcoal-900">{t('console.table_fulfillment')}</h3><span className="rounded-full bg-rice-100 px-3 py-1 text-xs font-bold text-charcoal-500">{tableLabel}</span></div>
            <div className="mt-4 grid grid-cols-2 gap-2">{stageKeys.map((value) => { const Icon = stageIcons[value]; return <button key={value} onClick={() => onStage(value)} className={`flex items-center gap-2 rounded-xl border p-3 text-left text-sm font-bold transition ${stage === value ? 'border-chili-500 bg-chili-50 text-chili-600' : 'border-charcoal-900/5 bg-rice-50 text-charcoal-500'}`}><Icon size={16} />{t(`console.stage.${value}`)}</button> })}</div>
          </section>
          <section className="rounded-2xl bg-white p-4 shadow-sm">
            <div className="flex items-center justify-between"><h3 className="font-bold text-charcoal-900">{t('console.service_response')}</h3><span className={`rounded-full px-3 py-1 text-xs font-bold ${waiting ? 'bg-amber-100 text-amber-500' : 'bg-emerald-50 text-emerald-600'}`}>{t('console.waiting_count', { count: waiting })}</span></div>
            <p className="mt-4 text-sm leading-6 text-charcoal-500">{t('console.response_desc')}</p>
            <Button onClick={onRespond} disabled={!waiting} variant="secondary" className="mt-3 w-full"><CheckCircle2 size={17} />{t('console.respond_btn')}</Button>
          </section>
        </div>
        <section className="mt-4 rounded-2xl bg-white p-4 shadow-sm">
          <div className="flex items-center justify-between"><h3 className="font-bold text-charcoal-900">{t('console.soldout_title')}</h3><span className="flex items-center gap-1 text-xs text-charcoal-500"><ToggleLeft size={16} />{t('console.soldout_hint')}</span></div>
          <div className="scrollbar-none mt-4 flex gap-2 overflow-x-auto pb-1">{products.map((product) => { const unavailable = soldOut.includes(product.id); return <button key={product.id} onClick={() => onSoldOut(product.id)} className={`flex shrink-0 items-center gap-2 rounded-xl border px-3 py-2 text-sm font-semibold ${unavailable ? 'border-chili-500/30 bg-chili-50 text-chili-600' : 'border-charcoal-900/5 bg-rice-50 text-charcoal-500'}`}>{unavailable ? <XCircle size={15} /> : <CheckCircle2 size={15} />}{t(product.name)}</button> })}</div>
        </section>
        <div className="mt-5 flex flex-col-reverse gap-3 sm:flex-row sm:justify-between"><Button variant="outline" onClick={onReset}><RotateCcw size={17} />{t('console.reset')}</Button><Button onClick={() => onOpenChange(false)}>{t('console.done')}</Button></div>
      </DialogContent>
    </Dialog>
  )
}
