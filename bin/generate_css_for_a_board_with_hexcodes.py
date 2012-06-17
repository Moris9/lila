#! /usr/bin/python2

#config
themes = {
  'grey': ['#fff', '#cacaca'],
  'green': ['#ffffdd', '#86a666'],
  'blue': ['#dee3e6', '#8ca2ad'],
  'brown': ['#f0d9b5', '#b58863']
}

blackPattern = 'body.{name} #GameBoard td.blackSquare, body.{name} #GameBoard td.highlightBlackSquare, body.{name} div.lcs.black, #top div.lcs.black.{name}, body.{name} span.theme_demo { background: {black}; }'
whitePattern = 'body.{name} #GameBoard td.whiteSquare, body.{name} #GameBoard td.highlightWhiteSquare, body.{name} div.lcs.white, #top div.lcs.white.{name} { background: {white}; }'

for name in themes:
  def formatCss(pattern):
    return pattern.replace('{name}', name).replace('{white}', themes[name][0]).replace('{black}', themes[name][1])
  print formatCss(whitePattern)
  print formatCss(blackPattern)
