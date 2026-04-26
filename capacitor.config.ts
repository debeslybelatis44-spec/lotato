import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.lotato.print',
  appName: 'LOTATO Print',
  webDir: 'www',
  server: {
    url: 'https://lotato1.onrender.com/test.html',
    cleartext: true
  },
  plugins: {
    SunmiPrinter: {
      bindOnLoad: true
    }
  }
};

export default config;
