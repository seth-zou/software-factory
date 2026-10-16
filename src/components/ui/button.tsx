import * as React from 'react'
import { cva, type VariantProps } from 'class-variance-authority'
import { cn } from '@/lib/utils'

const buttonVariants = cva(
  'inline-flex items-center justify-center gap-2 rounded-xl text-sm font-semibold transition-all focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-chili-500 disabled:pointer-events-none disabled:opacity-50 active:scale-95',
  {
    variants: {
      variant: {
        default: 'bg-chili-500 text-white shadow-md hover:bg-chili-600',
        secondary: 'bg-rice-200 text-charcoal-900 hover:bg-amber-100',
        outline: 'border border-charcoal-900/10 bg-white text-charcoal-900 hover:border-chili-500/30 hover:bg-chili-50',
        ghost: 'text-charcoal-700 hover:bg-rice-200',
      },
      size: { default: 'h-11 px-5', sm: 'h-9 rounded-lg px-3', icon: 'h-11 w-11 p-0' },
    },
    defaultVariants: { variant: 'default', size: 'default' },
  },
)

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement>, VariantProps<typeof buttonVariants> {}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(({ className, variant, size, ...props }, ref) => (
  <button ref={ref} className={cn(buttonVariants({ variant, size, className }))} {...props} />
))
Button.displayName = 'Button'
