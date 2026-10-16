import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, CreditCard, Gift, MessageCircleQuestion, ReceiptText, ShieldCheck, Smartphone } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { money } from '@/lib/utils'
import type { OrderItem } from '@/types'

interface CheckoutViewProps { items: OrderItem[]; paid: boolean; onPay: () => void; onBack: () => void }

export function CheckoutView({ items, paid, onPay, onBack }: CheckoutViewProps) {
  const { t } = useTranslation()
  const [method, setMethod] = useState('mobile')
  const subtotal = items.reduce((sum, item) => sum + item.price * item.quantity, 0)
  const discount = subtotal >= 100 ? 30 : 0
  const payable = subtotal - discount

  if (paid) return (
    <main className="mx-auto flex min-h-screen max-w-lg items-center px-5 py-10"><section className="w-full rounded-3xl bg-white p-7 text-center shadow-float"><span className="mx-auto flex h-20 w-20 items-center justify-center rounded-full bg-emerald-50 text-emerald-600"><Check size={36} /></span><p className="mt-6 text-sm font-bold text-emerald-600">{t('checkout.success_badge')}</p><h1 className="mt-2 text-3xl font-extrabold text-charcoal-900">{t('checkout.success_title')}</h1><p className="mt-3 whitespace-pre-line leading-7 text-charcoal-500">{t('checkout.success_desc')}</p><div className="mt-6 rounded-2xl bg-rice-100 p-4"><p className="text-sm text-charcoal-500">{t('checkout.paid')}</p><p className="mt-1 text-3xl font-extrabold text-chili-500">{money(payable)}</p></div><Button onClick={onBack} variant="outline" className="mt-6 w-full">{t('checkout.back')}</Button></section></main>
  )

  const methods = [
    { id: 'mobile', name: t('checkout.method_mobile'), icon: Smartphone, note: t('checkout.method_mobile_note') },
    { id: 'pos', name: t('checkout.method_pos'), icon: CreditCard, note: t('checkout.method_pos_note') },
  ]

  return (
    <main className="mx-auto max-w-4xl px-4 py-6 lg:py-10">
      <button onClick={onBack} className="mb-5 text-sm font-bold text-charcoal-500 hover:text-chili-500">← {t('checkout.back')}</button>
      <div className="grid gap-5 lg:grid-cols-5">
        <section className="rounded-3xl bg-white p-5 shadow-card lg:col-span-3">
          <div className="flex items-center gap-3"><span className="rounded-xl bg-chili-50 p-3 text-chili-500"><ReceiptText /></span><div><p className="text-xs font-bold text-chili-500">{t('checkout.badge')}</p><h1 className="text-2xl font-extrabold text-charcoal-900">{t('checkout.title')}</h1></div></div>
          <div className="mt-6 space-y-3">{items.map((item) => <div key={item.uid} className="flex justify-between text-sm"><span className="text-charcoal-700">{item.name} <small className="text-charcoal-500">× {item.quantity}</small></span><span className="font-semibold text-charcoal-900">{money(item.price * item.quantity)}</span></div>)}</div>
          <div className="mt-5 border-t border-dashed border-charcoal-900/10 pt-4"><div className="flex justify-between text-sm text-charcoal-500"><span>{t('checkout.subtotal')}</span><span>{money(subtotal)}</span></div><div className="mt-3 flex justify-between text-sm text-chili-500"><span className="flex items-center gap-2"><Gift size={15} />{t('checkout.discount')}</span><span>-{money(discount)}</span></div><div className="mt-4 flex items-end justify-between text-charcoal-900"><strong>{t('checkout.payable')}</strong><strong className="text-3xl text-chili-500">{money(payable)}</strong></div></div>
        </section>
        <section className="rounded-3xl bg-white p-5 shadow-card lg:col-span-2"><h2 className="font-extrabold text-charcoal-900">{t('checkout.select_method')}</h2><div className="mt-4 space-y-3">{methods.map(({ id, name, icon: Icon, note }) => <button key={id} onClick={() => setMethod(id)} className={`flex w-full items-center gap-3 rounded-2xl border p-4 text-left transition ${method === id ? 'border-chili-500 bg-chili-50' : 'border-charcoal-900/5 bg-rice-50'}`}><span className="rounded-xl bg-white p-2 text-chili-500"><Icon size={20} /></span><span className="flex-1"><strong className="block text-sm text-charcoal-900">{name}</strong><small className="text-charcoal-500">{note}</small></span>{method === id && <Check size={18} className="text-chili-500" />}</button>)}</div><Button onClick={onPay} className="mt-5 w-full"><ShieldCheck size={17} />{t('checkout.confirm_pay', { amount: money(payable) })}</Button><button className="mt-4 flex w-full items-center justify-center gap-2 text-xs font-semibold text-charcoal-500"><MessageCircleQuestion size={14} />{t('checkout.question')}</button></section>
      </div>
    </main>
  )
}
