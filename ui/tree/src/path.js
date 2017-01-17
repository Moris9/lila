module.exports = {

  root: '',

  size: function(path) {
    return path.length / 2;
  },

  head: function(path) {
    return path.slice(0, 2);
  },

  tail: function(path) {
    return path.slice(2);
  },

  init: function(path) {
    return path.slice(0, -2);
  },

  last: function(path) {
    return path.slice(-2);
  },

  contains: function(p1, p2) {
    return p1.indexOf(p2) === 0;
  },

  fromNodeList: function(nodes) {
    return nodes.map(function(n) {
      return n.id;
    }).join('');
  },

  isChildOf: function(child, parent) {
    return child && child.slice(0, -2) === parent;
  }
};
