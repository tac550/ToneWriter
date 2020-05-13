\version "2.18.2"

\language english

\header {
  subtitle = ""
  composer = ""
}

global = {
  \key c \major
  \time 1/4
}

noteHide = {\once \hide Stem \once \hide NoteHead \once \hide Accidental \once \override NoteHead.no-ledgers = ##t}

soprano = {
  \global
  c''4

  -\tweak layer #-1
   -\markup {
     \with-dimensions #'(0 . 0) #'(0 . 0)
     % specify color
     \with-color #(rgb-color 0.345 0.361 0.373)
     % specify size
     \filled-box #'(-1000 . 1000) #'(-1000 . 4000) #0
   }

  
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

verse = \lyricmode {



}

\score {
  \new ChoirStaff <<
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
