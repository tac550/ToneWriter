\version "2.18.2"

#(set-default-paper-size "letter")

\language english

\header {
  subtitle = ""
  subsubtitle = ""
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
      defaultBarType = "" % Hides the many barlines generated by having a 1/4 time signature
      \remove "Bar_number_engraver" % removes the bar numbers at the start of each system
    }
  }
  
}


  

