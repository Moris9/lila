// [combo threshold, time bonus]
const realLevels = [
  [0, 0],
  [5, 5],
  [12, 10],
  [20, 15],
  [30, 20]
];
const quickLevels = [
  [0, 0],
  [3, 5],
  [9, 10],
  [12, 15],
  [15, 20]
];

const config = {
  // all times in seconds
  clock: {
    initial: 3 * 60,
    // initial: 10,
    malus: 10
  },
  combo: {
    levels: realLevels,
    // levels: quickLevels,
  }
};

export default config;
