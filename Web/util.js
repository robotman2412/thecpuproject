
function escapeHtml(unsafe) {
	return unsafe
		.replace(/&/g, "&amp;")
		.replace(/</g, "&lt;")
		.replace(/>/g, "&gt;")
		.replace(/"/g, "&quot;")
		.replace(/'/g, "&#039;");
}

function formatMinutes(seconds) {
	var s = seconds % 60;
	var m = (seconds - s) / 60;
	if (s < 10) {
		s = "0" + s;
	}
	if (m < 10) {
		m = "0" + m;
	}
	return m + ":" + s;
}



