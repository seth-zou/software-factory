import { test, expect, type Page } from '@playwright/test'

/** Navigate from app start to the menu view and open a broth product spec dialog. */
async function goToBrothSpec(page: Page, productIndex = 0) {
  await page.goto('/')
  // Bind table
  await page.getByRole('button', { name: /A08/ }).first().click()
  // Welcome page - enter menu
  await page.getByRole('button', { name: /进入点餐|Enter/ }).click()
  // Switch to broth category
  await page.getByRole('button', { name: '锅底' }).click()
  // Open the broth product spec dialog
  const productCards = page.locator('article')
  await productCards.nth(productIndex).locator('button').last().click()
}

test.describe('超级辣锅底选项 - E2E 验收测试', () => {
  test('REQ-001: 超级辣选项在锅底辣度区域可见', async ({ page }) => {
    await goToBrothSpec(page, 0)
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).toBeVisible()
    const spicyButtons = page.locator('text=选择辣度').locator('..').locator('button')
    const texts = await spicyButtons.allTextContents()
    expect(texts).toEqual(['微辣', '中辣', '重辣', '超级辣'])
  })

  test('REQ-002: 选择超级辣时弹出风险提示弹窗，弹窗文案正确', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '超级辣' }).click()
    const warningText = page.getByText('您选择的「超级辣」辣度极高，可能对您的肠胃造成明显不适。请确认您能接受此辣度后再继续下单。')
    await expect(warningText).toBeVisible()
    await expect(page.getByRole('button', { name: '我已了解，继续下单' })).toBeVisible()
    await expect(page.getByRole('button', { name: '重新选择' })).toBeVisible()
  })

  test('REQ-002.3: 选择微辣/中辣/重辣时不弹出风险提示弹窗', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '中辣' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    await page.getByRole('button', { name: '重辣' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    await page.getByRole('button', { name: '微辣' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
  })

  test('REQ-002.4: 每次选择超级辣都弹出风险提示弹窗', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
    await page.getByRole('button', { name: '我已了解，继续下单' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
  })

  test('REQ-003: 确认后超级辣被选中并可继续下单', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
    await page.getByRole('button', { name: '我已了解，继续下单' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).toHaveClass(/border-chili-500/)
    await page.getByRole('button', { name: '加入本桌购物车' }).click()
    await expect(page.getByText('本桌购物车').first()).toBeVisible()
    await expect(page.getByText('超级辣').first()).toBeVisible()
    await expect(page.getByText('客户已确认风险').first()).toBeVisible()
  })

  test('REQ-004.1: 点击「重新选择」后超级辣不被选中，恢复之前选择', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '中辣' }).click()
    const mediumBtn = page.getByRole('button', { name: '中辣' })
    await expect(mediumBtn).toHaveClass(/border-chili-500/)
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
    await page.getByRole('button', { name: '重新选择' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).not.toHaveClass(/border-chili-500/)
    await expect(mediumBtn).toHaveClass(/border-chili-500/)
  })

  test('REQ-004.2: 点击遮罩区域等同取消，恢复原选择状态', async ({ page }) => {
    await goToBrothSpec(page, 0)
    // Select 微辣 first
    await page.getByRole('button', { name: '微辣' }).click()
    const mildBtn = page.getByRole('button', { name: '微辣' })
    await expect(mildBtn).toHaveClass(/border-chili-500/)
    // Click 超级辣
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
    // Click the risk warning overlay (last data-state=open overlay = risk warning's overlay)
    // Use force:true to bypass actionability checks since overlay intercepts events by design
    const overlay = page.locator('[data-state="open"][aria-hidden="true"]').last()
    await overlay.click({ force: true, position: { x: 5, y: 5 } })
    // Dialog closed
    await expect(page.getByText('风险提示')).not.toBeVisible()
    // 微辣 still selected, 超级辣 not selected
    await expect(mildBtn).toHaveClass(/border-chili-500/)
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).not.toHaveClass(/border-chili-500/)
  })

  test('REQ-004.4: 此前未选任何辣度时取消，恢复为无辣度选中状态', async ({ page }) => {
    await goToBrothSpec(page, 1)
    await page.getByRole('button', { name: '超级辣' }).click()
    await expect(page.getByText('风险提示')).toBeVisible()
    await page.getByRole('button', { name: '重新选择' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    const mildBtn = page.getByRole('button', { name: '微辣' })
    await expect(mildBtn).toHaveClass(/border-chili-500/)
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).not.toHaveClass(/border-chili-500/)
  })

  test('REQ-005: 现有辣度选项选择和下单流程不受影响', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await page.getByRole('button', { name: '重辣' }).click()
    await expect(page.getByText('风险提示')).not.toBeVisible()
    const heavyBtn = page.getByRole('button', { name: '重辣' })
    await expect(heavyBtn).toHaveClass(/border-chili-500/)
    await page.getByRole('button', { name: '加入本桌购物车' }).click()
    await expect(page.getByText('本桌购物车').first()).toBeVisible()
    await expect(page.getByText('重辣').first()).toBeVisible()
    await expect(page.getByText('客户已确认风险')).not.toBeVisible()
  })

  test('REQ-001: p2 牛油麻辣锅也显示超级辣选项', async ({ page }) => {
    await goToBrothSpec(page, 1)
    const superSpicyBtn = page.getByRole('button', { name: '超级辣' })
    await expect(superSpicyBtn).toBeVisible()
    await superSpicyBtn.click()
    await expect(page.getByText('风险提示')).toBeVisible()
  })

  test('NFR-001: 点餐页面和下单流程保持正常', async ({ page }) => {
    await goToBrothSpec(page, 0)
    await expect(page.getByRole('heading', { name: '鎏金番茄鸳鸯锅' })).toBeVisible()
    await page.keyboard.press('Escape')
    const productCards = page.locator('article')
    await productCards.nth(1).locator('button').last().click()
    await expect(page.getByRole('heading', { name: '牛油麻辣锅' })).toBeVisible()
    await expect(page.getByRole('button', { name: '微辣' })).toBeVisible()
    await expect(page.getByRole('button', { name: '中辣' })).toBeVisible()
    await expect(page.getByRole('button', { name: '重辣' })).toBeVisible()
    await expect(page.getByRole('button', { name: '超级辣' })).toBeVisible()
  })
})
