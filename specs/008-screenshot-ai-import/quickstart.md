# 快速验证：截图 OCR 与 AI 拆分导入

```powershell
cd backend
./mvnw -pl kb-app -am `
  "-Dtest=ScreenshotTextExtractorTest,ScreenshotImportServiceTest,ScreenshotImportControllerTest,TesseractOcrServiceTest" `
  "-Dsurefire.failIfNoSpecifiedTests=false" test

cd ../frontend/kb-web
pnpm lint
pnpm typecheck
pnpm test
pnpm build
pnpm exec playwright test e2e/app.spec.ts --workers=1
pnpm exec playwright test screenshot-import-taste.spec.ts --config ../../tests/playwright.config.ts --workers=1
```

## 真实浏览器验证

1. 复制一张含清晰中文文字的截图。
2. 进入 `/kb/capture`，选择“截图”。
3. 在粘贴区按 `Ctrl+V` 或系统粘贴菜单。
4. 确认出现预览和外部 AI 同意控件。
5. 勾选同意并点击“识别并拆分”。
6. 确认进入知识库，详情中没有原截图附件，正文可检索。

## 负向验证

- 不粘贴截图时按钮禁用。
- 粘贴后不勾选同意时按钮禁用。
- 1×1 空白截图返回 `SCREENSHOT_NO_TEXT`。
- 损坏图片返回 `SCREENSHOT_IMAGE_INVALID`。
- 未计费生产环境返回 `402 AI_BILLING_REQUIRED`。
