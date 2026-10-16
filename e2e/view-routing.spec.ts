import { test, expect, type Page } from '@playwright/test'

/** 从首页绑定 A08 桌台并进入点餐视图（menu）。 */
async function enterMenu(page: Page) {
  await page.goto('/')
  await page.getByRole('button', { name: /A08/ }).first().click() // home → welcome
  await page.getByRole('button', { name: /进入点餐|Enter/ }).click() // welcome → menu
}

test.describe('视图 URL 路由 - hash 模式', () => {
  test('ROUTE-001: 首页地址为 #/home', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/#\/home$/)
  })

  test('ROUTE-002: 主流程各视图拥有独立地址', async ({ page }) => {
    await page.goto('/')
    await page.getByRole('button', { name: /A08/ }).first().click()
    await expect(page).toHaveURL(/#\/welcome$/)
    await page.getByRole('button', { name: /进入点餐|Enter/ }).click()
    await expect(page).toHaveURL(/#\/menu$/)
  })

  test('ROUTE-003: 顶栏导航切换地址（menu ↔ order）', async ({ page }) => {
    await enterMenu(page)
    await page.getByRole('button', { name: /订单|Orders/ }).first().click()
    await expect(page).toHaveURL(/#\/order$/)
    await page.getByRole('button', { name: /点餐|Menu/ }).first().click()
    await expect(page).toHaveURL(/#\/menu$/)
  })

  test('ROUTE-004: 浏览器后退回到上一视图', async ({ page }) => {
    await enterMenu(page)
    await page.getByRole('button', { name: /订单|Orders/ }).first().click()
    await expect(page).toHaveURL(/#\/order$/)
    await page.goBack()
    await expect(page).toHaveURL(/#\/menu$/)
    await expect(page.getByRole('heading', { name: '鎏金番茄鸳鸯锅' })).toBeVisible()
  })

  test('ROUTE-005: 深链接直达 #/menu（自动绑定示例桌台）', async ({ page }) => {
    await page.goto('/#/menu')
    await expect(page).toHaveURL(/#\/menu$/)
    await expect(page.getByRole('heading', { name: '鎏金番茄鸳鸯锅' })).toBeVisible()
  })

  test('ROUTE-006: 非法地址回落到首页 #/home', async ({ page }) => {
    await page.goto('/#/not-a-view')
    await expect(page).toHaveURL(/#\/home$/)
    await expect(page.getByRole('button', { name: /A08/ }).first()).toBeVisible()
  })
})
