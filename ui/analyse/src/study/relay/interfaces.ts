export interface RelayData {
  sync: RelaySync;
  id: string;
  slug: string;
}

export interface RelaySync {
  seconds?: number; // how long until lichess stops syncing
  url: string;
  log: LogEvent[];
}

export interface LogEvent {
  moves: number;
  error?: string;
  at: number;
}
