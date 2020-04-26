export default function changeColorHandle(){

	const listClass: string[] = $('body').attr('class').split(' ')

	const dict: {[color: string]: [string, string]} = {
		"blue": ["#DEE3E6", "#788a94"],
		"blue2": ["#97b2c7", "#546f82"],
		"blue3": ["#d9e0e6", "#315991"],
		"canvas": ["#d7daeb", "#547388"],
		"wood": ["#d8a45b", "#9b4d0f"],
		"wood2": ["#a38b5d", "#6c5017"],
		"wood3": ["#d0ceca", "#755839"],
		"maple": ["#e8ceab", "#bc7944"],
		"maple2": ["#E2C89F", "#996633"],
		"leather": ["#d1d1c9", "#c28e16"],
		"green": ["#FFFFDD", "#6d8753"],
		"brown": ["#F0D9B5", "#946f51"],
		"pink": ["#E8E9B7", "#ED7272"],
		"marble": ["#93ab91", "#4f644e"],
		"blue-marble": ["#EAE6DD", "#7C7F87"],
		"green-plastic": ["#f2f9bb", "#59935d"],
		"green-glass": ["#fafa97", "#e4a836"],
		"grey": ["#b8b8b8", "#7d7d7d"],
		"metal": ["#c9c9c9", "#727272"],
		"olive": ["#b8b19f", "#6d6655"],
		"newspaper": ["#fff", "#8d8d8d"],
		"purple": ["#9f90b0", "#7d4a8d"],
		"purple-diag": ["#E5DAF0", "#957AB0"],
		"ic": ["#ececec", "#c1c18e"]
	}
	let color: string
	for (color of listClass){
		if (color in dict){
			document.documentElement.style.setProperty('--color-white', dict[color][0]);
			document.documentElement.style.setProperty('--color-black', dict[color][1]);
			document.documentElement.style.setProperty('--shadow', "none");
		}
	}
}
