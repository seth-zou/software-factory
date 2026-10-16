import { useTranslation } from 'react-i18next'
import { Check, Minus, Plus, ShoppingBasket, Trash2, Users } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { money } from '@/lib/utils'
import type { CartItem } from '@/types'

interface CartPanelProps {
  items: CartItem[]
  compact?: boolean
  onQuantity: (uid: string, delta: number) => void
  onSubmit: () => void
}

export function CartPanel({ items, compact, onQuantity, onSubmit }: CartPanelProps) {
  const { t } = useTranslation()
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  const people = [...new Set(items.map((item) => item.orderedBy))]

  if (!items.length) {
    return (
      <section className="rounded-3xl border border-charcoal-900/5 bg-white p-6 text-center shadow-card">
        <span className="mx-auto flex h-14 w-14 items-center justify-center rounded-2xl bg-rice-100 text-chili-500"><ShoppingBasket size={26} /></span>
        <h3 className="mt-4 font-bold text-charcoal-900">{t('cart.empty_title')}</h3>
        <p className="mt-2 whitespace-pre-line text-sm leading-6 text-charcoal-500">{t('cart.empty_desc')}</p>
      </section>
    )
  }

  return (
    <section className={`rounded-3xl border border-charcoal-900/5 bg-white shadow-card ${compact ? 'p-4' : 'p-5'}`}>
      <div className="flex items-center justify-between">
        <div><h3 className="text-lg font-extrabold text-charcoal-900">{t('cart.title')}</h3><p className="mt-1 text-xs text-charcoal-500">{t('cart.item_count', { count: items.reduce((sum, item) => sum + item.quantity, 0) })}</p></div>
        <span className="flex items-center gap-1 rounded-full bg-amber-100 px-3 py-1 text-xs font-bold text-charcoal-700"><Users size={13} />{t('cart.people', { count: people.length })}</span>
      </div>
      <div className="mt-5 space-y-4">
        {items.map((item) => (
          <div key={item.uid} className="group flex gap-3 border-b border-charcoal-900/5 pb-4 last:border-0">
            <img src={item.image} alt={item.name} className="h-16 w-16 rounded-xl object-cover" />
            <div className="min-w-0 flex-1">
              <div className="flex justify-between gap-2"><p className="truncate font-bold text-charcoal-900">{item.name}</p><strong className="text-sm text-chili-500">{money(item.price * item.quantity)}</strong></div>
              <p className="mt-1 truncate text-xs text-charcoal-500">{item.spec}</p>
              <div className="mt-2 flex items-center justify-between">
                <span className="flex items-center gap-1 text-xs font-semibold text-charcoal-500"><span className="h-5 w-5 rounded-full bg-amber-100 text-center leading-5 text-amber-500">{item.orderedBy.slice(0, 1)}</span>{t('cart.ordered_by', { name: item.orderedBy })}</span>
                <div className="flex items-center gap-2 rounded-lg bg-rice-100 p-1">
                  <button onClick={() => onQuantity(item.uid, -1)} className="rounded-md bg-white p-1 text-charcoal-700 shadow-sm" aria-label={t('common.aria_reduce')}>{item.quantity === 1 ? <Trash2 size={13} /> : <Minus size={13} />}</button>
                  <span className="w-4 text-center text-xs font-bold text-charcoal-900">{item.quantity}</span>
                  <button onClick={() => onQuantity(item.uid, 1)} className="rounded-md bg-chili-500 p-1 text-white" aria-label={t('common.aria_increase')}><Plus size={13} /></button>
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
      <div className="mt-5 rounded-2xl bg-rice-100 p-4">
        <div className="flex justify-between text-sm text-charcoal-500"><span>{t('cart.subtotal')}</span><span>{money(subtotal)}</span></div>
        <div className="mt-2 flex justify-between font-extrabold text-charcoal-900"><span>{t('cart.estimated')}</span><span className="text-xl text-chili-500">{money(subtotal)}</span></div>
      </div>
      <Button onClick={onSubmit} className="mt-4 w-full"><Check size={17} />{compact ? t('cart.submit_new') : t('cart.submit')}</Button>
      <p className="mt-3 text-center text-xs text-charcoal-500">{t('cart.submit_hint')}</p>
    </section>
  )
}
