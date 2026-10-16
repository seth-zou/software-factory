import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export const money = (value: number) => `¥${value.toFixed(2)}`

// crypto.randomUUID 仅在安全上下文（https / localhost）可用；
// 通过局域网 IP 或 file:// 打开时它是 undefined，直接调用会抛错。
// 演示为纯前端应用，需在任意来源下都能生成唯一 id，故降级为 Math.random 方案。
export function uid(): string {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID()
    }
  } catch {
    // 忽略：不可用时走下方降级分支
  }
  return `id-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`
}
