package com.focuslock.app.ui.components

private val QUOTES = listOf(
    "Small steps every day beat big plans someday.",
    "Today's effort is tomorrow's result.",
    "Focus now, relax later.",
    "One more hour of study is one step closer to your dream.",
    "Your phone can wait. Your future cannot.",
    "Discipline is choosing what you want most over what you want now.",
    "Do it tired. Do it bored. Just do it.",
    "Be proud of the hours nobody sees.",
    "Hard work looks boring until the result arrives.",
    "You do not need more time. You need more focus.",
    "Start before you feel ready.",
    "Every session you finish makes you stronger.",
    "Distractions are loud. Dreams are louder.",
    "Make today count.",
    "The best time to study was yesterday. The next best time is now.",
    "Consistency beats talent when talent gets lazy.",
    "Turn off the noise. Turn on the focus.",
    "Your streak is your proof.",
    "Progress, not perfection.",
    "Win the morning, win the day.",
    "A little more focus, a lot more success.",
    "You are one good session away from a better day.",
    "Stay hungry for the result, not the scroll.",
    "Big goals are built from quiet hours.",
    "Be the student you wish you had studied with.",
    "Sweat in silence, let success make the noise.",
    "Patience plus practice equals progress.",
    "Do not stop when you are tired. Stop when you are done.",
    "Your future self is watching. Make them proud.",
    "Work hard now, enjoy the result for years.",
    "Believe in the work you put in."
)

/** One line per day, the same all day, changing at midnight. */
fun dailyQuote(): String {
    val cal = java.util.Calendar.getInstance()
    val day = cal.get(java.util.Calendar.YEAR) * 366 + cal.get(java.util.Calendar.DAY_OF_YEAR)
    return QUOTES[day % QUOTES.size]
}
