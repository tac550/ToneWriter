\version "2.20.0"

#(set-default-paper-size "letter")

\language english

\paper {
  print-page-number = ##f
  oddFooterMarkup = \markup {
    \fill-line { "" \fromproperty #'page:page-number-string "" } }
  evenFooterMarkup = \oddFooterMarkup
  ragged-bottom = ##t
  top-margin = 13.0\mm
  bottom-margin = 13.0\mm
  left-margin = 13.0\mm
  right-margin = 13.0\mm
}

noteHide = {\once \hide Stem \once \hide NoteHead \once \hide Accidental \once \override NoteHead.no-ledgers = ##t}

lyricBold = {
  \override Lyrics.LyricText.font-series = #'bold
}
lyricItalic = {
  \override Lyrics.LyricText.font-shape = #'italic
}
lyricRevert = {
  \revert Lyrics.LyricText.font-series
  \revert Lyrics.LyricText.font-shape
}
