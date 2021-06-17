import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessPuzzle',
    input: 'src/main.ts',
    output: 'puzzle',
  },
  dashboard: {
    name: 'LichessPuzzleDashboard',
    input: 'src/dashboard.ts',
    output: 'puzzle.dashboard',
  },
  nvui: {
    name: 'NVUI',
    input: 'src/plugins/nvui.ts',
    output: 'puzzle.nvui',
  },
});
