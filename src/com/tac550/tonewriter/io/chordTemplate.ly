\version "2.20.0"

#(ly:set-option 'aux-files #f)
#(set-default-paper-size "a9landscape")

\language english

\header {
  subtitle = ""
  composer = ""
  tagline = ##f
}

global = {
  \key c \major
  \time 1/4
}

soprano = {
  \global
  c''4
}

alto = {
  \global
  c'4
}

tenor = {
  \global
  c'4
}

bass = {
  \global
  c4
}

verse = \lyricmode { }

\score {
  \new PianoStaff <<
    \new Staff \with {
      \once \override Staff.TimeSignature #'stencil = ##f % Hides the time signatures in the upper staves
      midiInstrument = #"choir aahs"
    } <<
      \new Voice = "soprano" { \voiceOne \soprano }
      \new Voice { \voiceTwo \alto }
    >>
    \new Lyrics \with {
      \override VerticalAxisGroup #'staff-affinity = #CENTER
    } \lyricsto "soprano" \verse
    
    \new Staff \with {
      \once \override Staff.TimeSignature #'stencil = ##f % Hides the time signatures in the lower staves
      midiInstrument = #"choir aahs"
    } <<
      \clef bass
      \new Voice { \voiceOne \tenor }
      \new Voice { \voiceTwo \bass }
    >>
  >>
  
  \layout {
    \context {
      \Score
      defaultBarType = "" % Hides any auto-generated barlines
      \remove "Bar_number_engraver" % removes the bar numbers at the start of each system
    }
  }
  
  \midi { }
}
