import { app, BrowserWindow } from 'electron';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// 1. Single Instance Lock: Prevent multiple copies and zombie processes
const gotTheLock = app.requestSingleInstanceLock();
if (!gotTheLock) {
  // Another instance is already running; terminate this one immediately
  app.exit(0);
}

let mainWindow = null;

// Handle second instance launch: bring existing window to focus
app.on('second-instance', () => {
  if (mainWindow) {
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.show();
    mainWindow.focus();
  }
});

// Fast startup Chromium switches
app.commandLine.appendSwitch('enable-gpu-rasterization');
app.commandLine.appendSwitch('enable-zero-copy');

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 1080,
    minHeight: 720,
    title: 'QuickBite | Campus Food Marketplace',
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      sandbox: true,
      spellcheck: false
    },
    backgroundColor: '#0F1115',
    show: false
  });

  mainWindow.removeMenu();

  const devUrl = 'http://127.0.0.1:5173';
  const isDev = process.env.NODE_ENV !== 'production' && !app.isPackaged;

  if (isDev) {
    mainWindow.loadURL(devUrl).catch(() => {
      mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
    });
  } else {
    mainWindow.loadFile(path.join(__dirname, '../dist/index.html'));
  }

  // Developer tools toggle (F12 or Ctrl+Shift+I)
  mainWindow.webContents.on('before-input-event', (event, input) => {
    if (input.key === 'F12' || (input.control && input.shift && input.key.toLowerCase() === 'i')) {
      mainWindow.webContents.toggleDevTools();
      event.preventDefault();
    }
  });

  // 2. Guaranteed Window Display: Never leave the window invisible in the background
  const showWindowSafely = () => {
    if (mainWindow && !mainWindow.isDestroyed() && !mainWindow.isVisible()) {
      mainWindow.show();
      mainWindow.focus();
    }
  };

  mainWindow.once('ready-to-show', showWindowSafely);
  // Fallback: If network fonts or scripts delay ready-to-show, force display within 700ms
  const fallbackShowTimer = setTimeout(showWindowSafely, 700);

  mainWindow.on('closed', () => {
    clearTimeout(fallbackShowTimer);
    mainWindow = null;
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// 3. Clean Process Termination: Ensure all helper processes exit cleanly
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('before-quit', () => {
  if (mainWindow && !mainWindow.isDestroyed()) {
    mainWindow.destroy();
  }
});

app.on('will-quit', () => {
  // Force clean exit of any lingering Chromium helper processes
  setTimeout(() => {
    app.exit(0);
  }, 100);
});

