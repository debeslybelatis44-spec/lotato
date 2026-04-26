import { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.lotato.print',
  appName: 'LOTATO Print',
  webDir: 'www',
  server: {
    url: 'https://lotato1.onrender.com',
    cleartext: true
  }
};

export default config;
